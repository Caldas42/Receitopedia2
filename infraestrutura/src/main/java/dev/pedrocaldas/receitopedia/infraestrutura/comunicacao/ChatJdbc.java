package dev.pedrocaldas.receitopedia.infraestrutura.comunicacao;

import static dev.pedrocaldas.receitopedia.aplicacao.comunicacao.DadosChat.ALTERADA;
import static dev.pedrocaldas.receitopedia.aplicacao.comunicacao.DadosChat.ENVIADA;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import dev.pedrocaldas.receitopedia.aplicacao.comunicacao.ChatServico;
import dev.pedrocaldas.receitopedia.aplicacao.comunicacao.DadosChat;
import dev.pedrocaldas.receitopedia.aplicacao.comunicacao.ConversasRepositorio;
import dev.pedrocaldas.receitopedia.aplicacao.comunicacao.DadosChat.Anexo;
import dev.pedrocaldas.receitopedia.aplicacao.comunicacao.DadosChat.Conversa;
import dev.pedrocaldas.receitopedia.aplicacao.comunicacao.DadosChat.Mensagem;
import dev.pedrocaldas.receitopedia.aplicacao.eventos.Evento;
import dev.pedrocaldas.receitopedia.aplicacao.eventos.RegistroEventos;
import dev.pedrocaldas.receitopedia.aplicacao.receitas.NovaFoto;
import dev.pedrocaldas.receitopedia.dominio.comunicacao.ConflitoMensagem;
import dev.pedrocaldas.receitopedia.dominio.comunicacao.ConteudoMensagem;
import dev.pedrocaldas.receitopedia.dominio.comunicacao.ConversaIndisponivel;
import dev.pedrocaldas.receitopedia.dominio.comunicacao.EstadoMensagem;
import dev.pedrocaldas.receitopedia.dominio.comunicacao.RegrasConversa;
import dev.pedrocaldas.receitopedia.dominio.receitas.TextosReceitas;
import dev.pedrocaldas.receitopedia.dominio.receitas.TipoFoto;
import dev.pedrocaldas.receitopedia.infraestrutura.fotos.FotosEmDisco;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;

@Repository
class ChatJdbc implements ConversasRepositorio {
    private static final String MENSAGENS = """
            select m.*, u.nome_usuario,
                exists (select 1 from chat_membro p
                    where p.conversa_id = m.conversa_id and p.ativo and p.usuario_id <> m.autor_id
                      and not exists (select 1 from chat_recibo r
                        where r.mensagem_id = m.id and r.usuario_id = p.usuario_id and r.lida_em is not null)) as leitura_pendente
            from chat_mensagem m join usuario u on u.id = m.autor_id
            """;

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate consultas;
    private final RegistroEventos eventos;
    private final FotosEmDisco arquivos;
    private final MiniaturasChat miniaturas;

    @PersistenceContext
    private EntityManager entidades;

    ChatJdbc(JdbcTemplate jdbc, NamedParameterJdbcTemplate consultas, RegistroEventos eventos, FotosEmDisco arquivos,
            MiniaturasChat miniaturas) {
        this.jdbc = jdbc;
        this.consultas = consultas;
        this.eventos = eventos;
        this.arquivos = arquivos;
        this.miniaturas = miniaturas;
    }

    @Override
    @Transactional
    public UUID abrirDm(UUID ator, String nomeChave) {
        UUID alvo = usuario(nomeChave);
        if (ator.equals(alvo)) {
            throw new IllegalArgumentException("Selecione outro usuário.");
        }
        var par = Par.de(ator, alvo);
        travarUsuarios(par);
        RegrasConversa.conferirBloqueio(bloqueado(par));
        var ids = jdbc.queryForList("select id from chat_conversa where dm_primeiro = ? and dm_segundo = ?",
                UUID.class, par.primeiro(), par.segundo());
        if (!ids.isEmpty()) {
            return ids.get(0);
        }
        UUID id = UUID.randomUUID();
        jdbc.update("insert into chat_conversa (id, tipo, dm_primeiro, dm_segundo) values (?, 'DM', ?, ?)",
                id, par.primeiro(), par.segundo());
        inserirMembro(id, ator, false);
        inserirMembro(id, alvo, false);
        avisar(id, null);
        return id;
    }

    @Override
    public List<Conversa> listar(UUID ator) {
        return consultas.query("""
                select c.id, c.tipo, coalesce(c.nome, u.nome_usuario) as nome, u.id as outro,
                    exists (select 1 from chat_bloqueio b where
                        (b.autor_id = :ator and b.alvo_id = u.id) or
                        (b.autor_id = u.id and b.alvo_id = :ator)) as bloqueada,
                    exists (select 1 from chat_bloqueio b where b.autor_id = :ator and b.alvo_id = u.id) as bloqueio_meu,
                    (select case when m.apagada then 'Mensagem apagada'
                        when m.texto = '' then 'Imagem' else m.texto end
                     from chat_mensagem m where m.conversa_id = c.id and (m.processada or m.autor_id = :ator)
                     order by m.sequencia desc limit 1) as ultima,
                    (select count(*) from chat_mensagem m where m.conversa_id = c.id and m.processada
                        and not m.apagada and m.autor_id <> :ator
                        and not exists (select 1 from chat_recibo r where r.mensagem_id = m.id
                             and r.usuario_id = :ator and r.lida_em is not null)) as nao_lidas
                from chat_membro p join chat_conversa c on c.id = p.conversa_id
                left join usuario u on u.id = case when c.dm_primeiro = :ator then c.dm_segundo else c.dm_primeiro end
                where p.usuario_id = :ator and p.ativo
                order by (select max(m.enviada_em) from chat_mensagem m where m.conversa_id = c.id
                    and (m.processada or m.autor_id = :ator)) desc nulls last, c.id
                """, Map.of("ator", ator), (rs, n) -> new Conversa(uuid(rs, "id"), rs.getString("tipo"),
                rs.getString("nome"), uuid(rs, "outro"), rs.getBoolean("bloqueada"), rs.getBoolean("bloqueio_meu"), rs.getString("ultima"),
                rs.getLong("nao_lidas")));
    }

    @Override
    @Transactional
    public Mensagem enviar(UUID ator, UUID conversa, UUID id, ConteudoMensagem conteudo, List<NovaFoto> anexos) {
        var inicial = acessar(ator, conversa, false);
        if (inicial.par() != null) {
            travarUsuarios(inicial.par());
            RegrasConversa.conferirBloqueio(bloqueado(inicial.par()));
        }
        var atual = acessar(ator, conversa, true);
        var existente = mensagens(MENSAGENS + " where m.id = ?", id);
        if (!existente.isEmpty()) {
            var linha = existente.get(0);
            Boolean mesmaConversa = jdbc.queryForObject("select conversa_id = ? from chat_mensagem where id = ?",
                    Boolean.class, conversa, id);
            if (Boolean.TRUE.equals(mesmaConversa) && linha.autorId().equals(ator)) {
                return anexar(existente).get(0);
            }
            throw new ConflitoMensagem("Identificador de mensagem indisponível.");
        }
        long sequencia = atual.sequencia() + 1;
        jdbc.update("update chat_conversa set ultima_seq = ? where id = ?", sequencia, conversa);
        try {
            jdbc.update("""
                    insert into chat_mensagem (id, conversa_id, autor_id, sequencia, texto, texto_chave, enviada_em)
                    values (?, ?, ?, ?, ?, ?, ?)
                    """, id, conversa, ator, sequencia, conteudo.texto(), TextosReceitas.normalizar(conteudo.texto()),
                    OffsetDateTime.now(ZoneOffset.UTC));
        } catch (DuplicateKeyException erro) {
            throw new ConflitoMensagem("Identificador de mensagem indisponível.");
        }
        for (var anexo : anexos) {
            jdbc.update("insert into chat_anexo (id, mensagem_id, tipo) values (?, ?, ?)",
                    anexo.foto().id(), id, anexo.foto().tipo().name());
        }
        arquivos.gravar(anexos);
        eventos.registrar(Evento.novo(ENVIADA, conversa, id));
        return anexar(mensagens(MENSAGENS + " where m.id = ?", id)).get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Mensagem> historico(UUID ator, UUID conversa, String trecho, long antes) {
        acessar(ator, conversa, false);
        String filtro = trecho.equals("%%") ? "" : " and not m.apagada and m.texto_chave like :trecho escape '!'";
        return anexar(consultas.query(MENSAGENS + """
                where m.conversa_id = :conversa and (m.processada or m.autor_id = :ator)
                and m.sequencia < :antes
                """ + filtro + " order by m.sequencia desc limit " + ChatServico.PAGINA,
                Map.of("ator", ator, "conversa", conversa, "antes", antes, "trecho", trecho), ChatJdbc::mensagem));
    }

    @Override
    @Transactional
    public void ler(UUID ator, UUID conversa, List<UUID> ids) {
        acessar(ator, conversa, true);
        if (ids.isEmpty()) {
            return;
        }
        var parametros = Map.of("ator", ator, "conversa", conversa, "ids", ids);
        consultas.update("""
                insert into chat_recibo (mensagem_id, usuario_id, entregue_em)
                select m.id, :ator, current_timestamp from chat_mensagem m
                where m.conversa_id = :conversa and m.processada and not m.apagada
                    and m.autor_id <> :ator and m.id in (:ids)
                on conflict do nothing
                """, parametros);
        int alteradas = consultas.update("""
                update chat_recibo set lida_em = current_timestamp
                where usuario_id = :ator and lida_em is null and mensagem_id in (
                    select id from chat_mensagem where conversa_id = :conversa and processada and not apagada
                    and autor_id <> :ator and id in (:ids))
                """, parametros);
        if (alteradas != 0) {
            avisar(conversa, null);
        }
    }

    @Override
    @Transactional
    public void bloquear(UUID ator, UUID alvo, boolean bloquear) {
        var par = Par.de(ator, alvo);
        travarUsuarios(par);
        if (bloquear) {
            jdbc.update("""
                    insert into chat_bloqueio (autor_id, alvo_id) select ?, ?
                    where not exists (select 1 from chat_bloqueio where autor_id = ? and alvo_id = ?)
                    """, ator, alvo, ator, alvo);
        } else {
            jdbc.update("delete from chat_bloqueio where autor_id = ? and alvo_id = ?", ator, alvo);
        }
        jdbc.queryForList("select id from chat_conversa where dm_primeiro = ? and dm_segundo = ?",
                UUID.class, par.primeiro(), par.segundo()).forEach(id -> avisar(id, null));
    }

    @Override
    @Transactional
    public UUID criarGrupo(UUID ator, String nome, List<String> usuarios) {
        var membros = new LinkedHashSet<UUID>();
        membros.add(ator);
        if (!usuarios.isEmpty()) {
            var ids = consultas.queryForList("select id from usuario where nome_chave in (:nomes)",
                    Map.of("nomes", usuarios), UUID.class);
            if (ids.size() != usuarios.size()) {
                throw new ConversaIndisponivel();
            }
            membros.addAll(ids);
        }
        RegrasConversa.conferirCapacidade(membros.size());
        UUID id = UUID.randomUUID();
        jdbc.update("insert into chat_conversa (id, tipo, nome) values (?, 'GRUPO', ?)", id, nome);
        jdbc.batchUpdate("insert into chat_membro (conversa_id, usuario_id, admin) values (?, ?, ?)",
                membros.stream().map(membro -> new Object[]{id, membro, membro.equals(ator)}).toList());
        avisar(id, null);
        return id;
    }

    @Override
    @Transactional(readOnly = true)
    public DadosChat.Grupo grupo(UUID ator, UUID conversa) {
        acessarGrupo(ator, conversa, false);
        var nome = jdbc.queryForObject("select nome from chat_conversa where id = ?", String.class, conversa);
        var membros = jdbc.query("""
                select u.id, u.nome_usuario, p.admin from chat_membro p join usuario u on u.id = p.usuario_id
                where p.conversa_id = ? and p.ativo order by u.nome_chave
                """, (rs, posicao) -> new DadosChat.Membro(uuid(rs, "id"), rs.getString("nome_usuario"),
                rs.getBoolean("admin")), conversa);
        return new DadosChat.Grupo(conversa, nome, membros);
    }

    @Override
    @Transactional
    public void renomearGrupo(UUID ator, UUID conversa, String nome) {
        acessarGrupo(ator, conversa, true);
        RegrasConversa.exigirAdmin(admin(ator, conversa));
        jdbc.update("update chat_conversa set nome = ? where id = ?", nome, conversa);
        avisar(conversa, null);
    }

    @Override
    @Transactional
    public void membro(UUID ator, UUID conversa, DadosChat.AcaoGrupo acao, String nome, UUID alvo) {
        acessarGrupo(ator, conversa, true);
        if (acao != DadosChat.AcaoGrupo.SAIR) {
            RegrasConversa.exigirAdmin(admin(ator, conversa));
        }
        if (acao == DadosChat.AcaoGrupo.ADICIONAR) {
            adicionarAoGrupo(conversa, usuario(nome));
            return;
        }
        UUID membro = acao == DadosChat.AcaoGrupo.SAIR ? ator : alvo;
        boolean adminDoAlvo = admin(membro, conversa);
        if (acao == DadosChat.AcaoGrupo.PROMOVER) {
            jdbc.update("update chat_membro set admin = true where conversa_id = ? and usuario_id = ?", conversa, membro);
        } else {
            long administradores = jdbc.queryForObject(
                    "select count(*) from chat_membro where conversa_id = ? and ativo and admin", Long.class, conversa);
            RegrasConversa.manterAdmin(adminDoAlvo, administradores);
            jdbc.update("update chat_membro set ativo = false, admin = false where conversa_id = ? and usuario_id = ?",
                    conversa, membro);
        }
        avisar(conversa, membro);
    }

    private void adicionarAoGrupo(UUID conversa, UUID alvo) {
        var vinculos = jdbc.queryForList("select ativo from chat_membro where conversa_id = ? and usuario_id = ?",
                Boolean.class, conversa, alvo);
        if (!vinculos.isEmpty() && vinculos.get(0)) {
            return;
        }
        long atuais = jdbc.queryForObject(
                "select count(*) from chat_membro where conversa_id = ? and ativo", Long.class, conversa);
        RegrasConversa.conferirCapacidade(atuais + 1);
        if (vinculos.isEmpty()) {
            inserirMembro(conversa, alvo, false);
        } else {
            jdbc.update("update chat_membro set ativo = true where conversa_id = ? and usuario_id = ?", conversa, alvo);
        }
        avisar(conversa, alvo);
    }

    private boolean admin(UUID usuario, UUID conversa) {
        var papeis = jdbc.queryForList("select admin from chat_membro where conversa_id = ? and usuario_id = ? and ativo",
                Boolean.class, conversa, usuario);
        if (papeis.isEmpty()) {
            throw new ConversaIndisponivel();
        }
        return papeis.get(0);
    }

    private void acessarGrupo(UUID ator, UUID conversa, boolean travar) {
        if (!"GRUPO".equals(acessar(ator, conversa, travar).tipo())) {
            throw new ConversaIndisponivel();
        }
    }

    @Override
    @Transactional
    public Mensagem alterar(UUID ator, UUID conversa, UUID id, long versao, String texto, boolean apagar) {
        acessar(ator, conversa, false);
        var mensagem = entidades.find(MensagemJpa.class, id);
        if (mensagem == null || !mensagem.conversaId().equals(conversa)) {
            throw new ConversaIndisponivel();
        }
        if (mensagem.autorId().equals(ator) && mensagem.versao() != versao) {
            throw new ConflitoMensagem("A mensagem já foi alterada. Atualize a conversa.");
        }
        RegrasConversa.conferirAlteracao(ator, mensagem.autorId(), mensagem.enviadaEm().toInstant(),
                java.time.Instant.now(), mensagem.apagada());
        if (apagar) {
            mensagem.apagar();
        } else {
            int anexos = jdbc.queryForObject("select count(*) from chat_anexo where mensagem_id = ?", Integer.class, id);
            var dados = new ConteudoMensagem(texto, anexos);
            mensagem.editar(dados.texto(), TextosReceitas.normalizar(dados.texto()));
        }
        try {
            entidades.flush();
        } catch (OptimisticLockException erro) {
            throw new ConflitoMensagem("A mensagem já foi alterada. Atualize a conversa.");
        }
        avisar(conversa, null);
        return anexar(mensagens(MENSAGENS + " where m.id = ?", id)).get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public DadosChat.Arquivo anexo(UUID ator, UUID conversa, UUID id, boolean miniatura) {
        acessar(ator, conversa, false);
        var tipos = jdbc.queryForList("""
                select a.tipo from chat_anexo a join chat_mensagem m on m.id = a.mensagem_id
                where a.id = ? and m.conversa_id = ? and not m.apagada and (m.processada or m.autor_id = ?)
                """, String.class, id, conversa, ator);
        if (tipos.isEmpty()) {
            throw new ConversaIndisponivel();
        }
        if (miniatura) {
            var imagem = miniaturas.ler(id);
            if (imagem.isPresent()) {
                return new DadosChat.Arquivo(TipoFoto.JPEG.mime(), imagem.get());
            }
        }
        byte[] bytes = arquivos.ler(id).orElseThrow(ConversaIndisponivel::new);
        return new DadosChat.Arquivo(TipoFoto.valueOf(tipos.get(0)).mime(), bytes);
    }

    @Override
    public List<UUID> participantes(UUID conversa) {
        return jdbc.queryForList("select usuario_id from chat_membro where conversa_id = ? and ativo", UUID.class, conversa);
    }

    Acesso acessar(UUID ator, UUID conversa, boolean travar) {
        // Bloqueie primeiro a conversa e confira o vínculo novamente após obter o lock.
        var resultados = jdbc.query("""
                select c.tipo, c.dm_primeiro, c.dm_segundo, c.ultima_seq
                from chat_conversa c where c.id = ?
                  and exists (select 1 from chat_membro p where p.conversa_id = c.id and p.usuario_id = ? and p.ativo)
                """ + (travar ? " for update" : ""), (rs, n) -> new Acesso(rs.getString("tipo"),
                uuid(rs, "dm_primeiro") == null ? null : new Par(uuid(rs, "dm_primeiro"), uuid(rs, "dm_segundo")),
                rs.getLong("ultima_seq")), conversa, ator);
        if (resultados.isEmpty() || (travar && jdbc.queryForObject("""
                select count(*) from chat_membro where conversa_id = ? and usuario_id = ? and ativo
                """, Long.class, conversa, ator) == 0)) {
            throw new ConversaIndisponivel();
        }
        return resultados.get(0);
    }

    UUID usuario(String nomeChave) {
        var usuarios = jdbc.queryForList("select id from usuario where nome_chave = ?", UUID.class, nomeChave);
        if (usuarios.isEmpty()) {
            throw new ConversaIndisponivel();
        }
        return usuarios.get(0);
    }

    void inserirMembro(UUID conversa, UUID usuario, boolean admin) {
        jdbc.update("insert into chat_membro (conversa_id, usuario_id, admin) values (?, ?, ?)", conversa, usuario, admin);
    }

    void avisar(UUID conversa, UUID afetado) {
        eventos.registrar(Evento.novo(ALTERADA, conversa, afetado));
    }

    List<Mensagem> mensagens(String sql, Object... parametros) {
        return jdbc.query(sql, (RowMapper<Mensagem>) ChatJdbc::mensagem, parametros);
    }

    List<Mensagem> anexar(List<Mensagem> mensagens) {
        if (mensagens.isEmpty()) {
            return mensagens;
        }
        var anexos = new HashMap<UUID, List<Anexo>>();
        consultas.query("""
                select a.* from chat_anexo a join chat_mensagem m on m.id = a.mensagem_id
                where a.mensagem_id in (:ids) and not m.apagada order by a.id
                """, Map.of("ids", mensagens.stream().map(Mensagem::id).toList()), rs -> {
            anexos.computeIfAbsent(uuid(rs, "mensagem_id"), id -> new ArrayList<>())
                    .add(new Anexo(uuid(rs, "id"), rs.getString("tipo"), rs.getBoolean("miniatura")));
        });
        return mensagens.stream().map(m -> new Mensagem(m.id(), m.sequencia(), m.autorId(), m.autor(), m.texto(),
                m.versao(), m.enviadaEm(), m.editada(), m.apagada(), m.status(),
                anexos.getOrDefault(m.id(), List.of()))).toList();
    }

    private void travarUsuarios(Par par) {
        int primeiro = jdbc.queryForList("select id from usuario where id = ? for update", UUID.class, par.primeiro()).size();
        int segundo = jdbc.queryForList("select id from usuario where id = ? for update", UUID.class, par.segundo()).size();
        if (primeiro + segundo != 2) {
            throw new ConversaIndisponivel();
        }
    }

    private boolean bloqueado(Par par) {
        return jdbc.queryForObject("""
                select count(*) from chat_bloqueio where
                  (autor_id = ? and alvo_id = ?) or (autor_id = ? and alvo_id = ?)
                """, Long.class, par.primeiro(), par.segundo(), par.segundo(), par.primeiro()) != 0;
    }

    private static Mensagem mensagem(ResultSet rs, int numero) throws SQLException {
        boolean apagada = rs.getBoolean("apagada");
        return new Mensagem(uuid(rs, "id"), rs.getLong("sequencia"), uuid(rs, "autor_id"), rs.getString("nome_usuario"),
                apagada ? "" : rs.getString("texto"), rs.getLong("versao"),
                rs.getObject("enviada_em", OffsetDateTime.class).toInstant(), rs.getBoolean("editada"),
                apagada, EstadoMensagem.calcular(rs.getBoolean("processada"), rs.getBoolean("leitura_pendente")).name(), List.of());
    }

    static UUID uuid(ResultSet rs, String coluna) throws SQLException {
        return rs.getObject(coluna, UUID.class);
    }

    record Acesso(String tipo, Par par, long sequencia) {}

    record Par(UUID primeiro, UUID segundo) {
        static Par de(UUID a, UUID b) {
            return a.compareTo(b) < 0 ? new Par(a, b) : new Par(b, a);
        }
    }
}

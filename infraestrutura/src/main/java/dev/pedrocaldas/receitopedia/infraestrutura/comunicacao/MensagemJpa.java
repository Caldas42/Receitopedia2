package dev.pedrocaldas.receitopedia.infraestrutura.comunicacao;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "chat_mensagem")
class MensagemJpa {
    @Id
    private UUID id;
    private UUID conversaId;
    private UUID autorId;
    private String texto;
    private String textoChave;
    private OffsetDateTime enviadaEm;
    private boolean editada;
    private boolean apagada;

    @Version
    private long versao;

    protected MensagemJpa() {
    }

    UUID conversaId() { return conversaId; }
    UUID autorId() { return autorId; }
    OffsetDateTime enviadaEm() { return enviadaEm; }
    boolean apagada() { return apagada; }
    long versao() { return versao; }

    void editar(String texto, String chave) {
        this.texto = texto;
        textoChave = chave;
        editada = true;
    }

    void apagar() {
        texto = "";
        textoChave = "";
        apagada = true;
    }
}

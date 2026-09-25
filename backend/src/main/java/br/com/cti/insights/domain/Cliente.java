package br.com.cti.insights.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "cliente", indexes = {
    @Index(name = "idx_cliente_consultor", columnList = "id_consultor"),
    @Index(name = "idx_cliente_segmento", columnList = "segmento"),
    @Index(name = "idx_cliente_nivel", columnList = "nivel")
})
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Long id;

    @Column(name = "codigo_cti", nullable = false, unique = true, length = 80)
    private String codigoCti;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_consultor", nullable = false)
    private Consultor consultor;

    @Column(name = "nome_empresa", nullable = false, length = 180)
    private String nomeEmpresa;

    @Column(nullable = false, length = 100)
    private String segmento;

    @Column(name = "faturamento_anual", nullable = false, precision = 15, scale = 2)
    private BigDecimal faturamentoAnual = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1)
    private NivelCliente nivel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusCliente status = StatusCliente.ATIVO;

    protected Cliente() {}

    public Cliente(String codigoCti, Consultor consultor, String nomeEmpresa, String segmento,
                   BigDecimal faturamentoAnual, NivelCliente nivel, StatusCliente status) {
        this.codigoCti = codigoCti;
        this.consultor = consultor;
        this.nomeEmpresa = nomeEmpresa;
        this.segmento = segmento;
        this.faturamentoAnual = faturamentoAnual;
        this.nivel = nivel;
        this.status = status;
    }

    public Long getId() { return id; }
    public String getCodigoCti() { return codigoCti; }
    public Consultor getConsultor() { return consultor; }
    public String getNomeEmpresa() { return nomeEmpresa; }
    public String getSegmento() { return segmento; }
    public BigDecimal getFaturamentoAnual() { return faturamentoAnual; }
    public NivelCliente getNivel() { return nivel; }
    public StatusCliente getStatus() { return status; }

    public void update(String codigoCti, Consultor consultor, String nomeEmpresa, String segmento,
                       BigDecimal faturamentoAnual, NivelCliente nivel, StatusCliente status) {
        this.codigoCti = codigoCti;
        this.consultor = consultor;
        this.nomeEmpresa = nomeEmpresa;
        this.segmento = segmento;
        this.faturamentoAnual = faturamentoAnual;
        this.nivel = nivel;
        this.status = status;
    }
}

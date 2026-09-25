package br.com.cti.insights.clients;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.cti.insights.domain.Cliente;
import br.com.cti.insights.domain.Consultor;
import br.com.cti.insights.domain.StatusCliente;

@Service
public class ClienteService {
    private final ClienteRepository clientes;
    private final ConsultorRepository consultores;

    public ClienteService(ClienteRepository clientes, ConsultorRepository consultores) {
        this.clientes = clientes;
        this.consultores = consultores;
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> list() {
        return clientes.findAll().stream().map(ClienteResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse get(Long id) {
        return ClienteResponse.from(find(id));
    }

    @Transactional
    public ClienteResponse create(ClienteRequest request) {
        if (clientes.existsByCodigoCti(request.codigoCti().trim())) {
            throw new ClienteDuplicadoException(request.codigoCti());
        }
        Cliente cliente = new Cliente(request.codigoCti().trim(), consultor(request.consultorId()),
            request.nomeEmpresa().trim(), request.segmento().trim(), request.faturamentoAnual(),
            request.nivel(), request.status() == null ? StatusCliente.ATIVO : request.status());
        return ClienteResponse.from(clientes.save(cliente));
    }

    @Transactional
    public ClienteResponse update(Long id, ClienteRequest request) {
        Cliente cliente = find(id);
        String codigo = request.codigoCti().trim();
        if (clientes.existsByCodigoCtiAndIdNot(codigo, id)) {
            throw new ClienteDuplicadoException(codigo);
        }
        cliente.update(codigo, consultor(request.consultorId()), request.nomeEmpresa().trim(),
            request.segmento().trim(), request.faturamentoAnual(), request.nivel(),
            request.status() == null ? StatusCliente.ATIVO : request.status());
        return ClienteResponse.from(clientes.save(cliente));
    }

    @Transactional
    public void delete(Long id) {
        clientes.delete(find(id));
    }

    private Cliente find(Long id) {
        return clientes.findById(id).orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado: " + id));
    }

    private Consultor consultor(Long id) {
        return consultores.findById(id).orElseThrow(() -> new IllegalArgumentException("Consultor não encontrado: " + id));
    }
}

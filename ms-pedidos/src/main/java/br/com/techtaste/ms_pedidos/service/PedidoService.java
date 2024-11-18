package br.com.techtaste.ms_pedidos.service;

import br.com.techtaste.ms_pedidos.dto.AutorizacaoDto;
import br.com.techtaste.ms_pedidos.dto.PedidoRequestDto;
import br.com.techtaste.ms_pedidos.dto.PedidoResponseDto;
import br.com.techtaste.ms_pedidos.model.Pedido;
import br.com.techtaste.ms_pedidos.model.Status;
import br.com.techtaste.ms_pedidos.repository.PedidoRepository;
import br.com.techtaste.ms_pedidos.utils.PagamentoClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PedidoService {

    private final PedidoRepository repository;
    private final PagamentoClient pagamentoClient;

    public PedidoService(PedidoRepository repository, PagamentoClient pagamentoClient) {
        this.repository = repository;
        this.pagamentoClient = pagamentoClient;
    }


    public PedidoResponseDto cadastrarPedido(PedidoRequestDto pedidoDto, boolean comErro) {
        Pedido pedido = new Pedido();
        BeanUtils.copyProperties(pedidoDto, pedido);
        pedido.setStatus(Status.AGUARDANDO_PAGAMENTO);
        pedido.setData(LocalDate.now());
        pedido.calcularTotal();
        repository.save(pedido);
        Status status;
        if (comErro) {
            status = Status.ERRO_CONSULTA_PGTO;
        } else {
            status = vericaAutorizacao(pedido.getId());
        }
        pedido.setStatus(status);
        repository.save(pedido);
        return new PedidoResponseDto(pedido.getId(), pedido.getStatus(),
                pedido.getCpf(), pedido.getItens(), pedido.getValorTotal(),
                pedido.getData());
    }

    private Status vericaAutorizacao(UUID id) {
       AutorizacaoDto dto = pagamentoClient.obterAutorizacao(id.toString());
       Status status = dto.status().equalsIgnoreCase("autorizado") ?
               Status.PREPARANDO : Status.RECUSASDO;
       return status;
    }

    public List<PedidoResponseDto> obterTodos() {
        return repository.findAll().stream()
                .map(pedido -> new PedidoResponseDto(pedido.getId(), pedido.getStatus(),
                        pedido.getCpf(), pedido.getItens(), pedido.getValorTotal(),
                        pedido.getData()))
                .collect(Collectors.toList());
    }
}

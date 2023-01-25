package com.dimotta.msavaliadorcredito.application;

import com.dimotta.msavaliadorcredito.application.ex.DadosClienteNotFoundException;
import com.dimotta.msavaliadorcredito.application.ex.ErroComunicacaoMicroservicesException;
import com.dimotta.msavaliadorcredito.application.ex.ErroSolicitacaoCartaoException;
import com.dimotta.msavaliadorcredito.domain.model.Cartao;
import com.dimotta.msavaliadorcredito.domain.model.CartaoAprovado;
import com.dimotta.msavaliadorcredito.domain.model.CartaoCliente;
import com.dimotta.msavaliadorcredito.domain.model.DadosCliente;
import com.dimotta.msavaliadorcredito.domain.model.DadosSolicitacaoEmissaoCartao;
import com.dimotta.msavaliadorcredito.domain.model.ProtocoloSolicitacaoCartao;
import com.dimotta.msavaliadorcredito.domain.model.RetornoAvaliacaoCliente;
import com.dimotta.msavaliadorcredito.domain.model.SituacaoCliente;
import com.dimotta.msavaliadorcredito.infra.clients.CartoesResourceClient;
import com.dimotta.msavaliadorcredito.infra.clients.ClienteResourceClient;
import com.dimotta.msavaliadorcredito.infra.mqueue.SolicitacaoEmissaoCartaoPublisher;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvaliadorCreditoService {

    private final ClienteResourceClient clientesClient;
    private final CartoesResourceClient cartoesClient;
    private final SolicitacaoEmissaoCartaoPublisher emissaoCartaoPublisher;

    public SituacaoCliente obterSituacaoCliente(String cpf)
            throws DadosClienteNotFoundException,
            ErroComunicacaoMicroservicesException {

        try {
            ResponseEntity<DadosCliente> dadosClienteResponse =
                    clientesClient.dadoscliente(cpf);

            ResponseEntity<List<CartaoCliente>> cartoesResponse =
                    cartoesClient.getCartoesByCliente(cpf);

            return SituacaoCliente
                    .builder()
                    .cliente(dadosClienteResponse.getBody())
                    .cartoes(cartoesResponse.getBody())
                    .build();
        } catch (FeignException.FeignClientException e) {
            int status = e.status();
            if(HttpStatus.NOT_FOUND.value() == status){
                throw new DadosClienteNotFoundException();
            }
            throw new ErroComunicacaoMicroservicesException(
                    e.getMessage(), status);
        }
    }
    public RetornoAvaliacaoCliente realizarAvaliacao(String cpf, Long renda)
    throws DadosClienteNotFoundException, ErroComunicacaoMicroservicesException {

        try {
            ResponseEntity<DadosCliente> dadosClienteResponse =
                    clientesClient.dadoscliente(cpf);

            ResponseEntity<List<Cartao>> cartoesResponse =
                    cartoesClient.getCartoesRendaAteh(renda);

            List<Cartao> cartoes = cartoesResponse.getBody();
            var listaCartoesAprovados =
                    cartoes.stream().map(cartao -> {
                DadosCliente dadosCliente = dadosClienteResponse.getBody();

                BigDecimal limiteBasico = cartao.getLimiteBasico();
                BigDecimal rendaBD = BigDecimal.valueOf(renda);
                BigDecimal idadeBD = BigDecimal.valueOf(
                        dadosCliente.getIdade());
                var fator = idadeBD.divide(BigDecimal.valueOf(10));
                BigDecimal limiteAprovado = fator.multiply(limiteBasico);

                CartaoAprovado aprovado = new CartaoAprovado();
                aprovado.setCartao(cartao.getNome());
                aprovado.setBandeira(cartao.getBandeira());
                aprovado.setLimiteAprovado(limiteAprovado);

                return aprovado;
            }).collect(Collectors.toList());

            return new RetornoAvaliacaoCliente(listaCartoesAprovados);

        } catch (FeignException.FeignClientException e) {
            int status = e.status();
            if (HttpStatus.NOT_FOUND.value() == status) {
                throw new DadosClienteNotFoundException();
            }
            throw new ErroComunicacaoMicroservicesException(
                    e.getMessage(), status);
        }
    }

    public ProtocoloSolicitacaoCartao solicitarEmissaoCartao(
            DadosSolicitacaoEmissaoCartao dados) {
        try {
            emissaoCartaoPublisher.solicitarCartao(dados);
            var protocolo = UUID.randomUUID().toString();
            return new ProtocoloSolicitacaoCartao(protocolo);

        }catch (Exception e){
            throw new ErroSolicitacaoCartaoException(e.getMessage());

        }
    }
}

package com.dimotta.mscartoes.infra.mqueue;

import com.dimotta.mscartoes.domain.Cartao;
import com.dimotta.mscartoes.domain.ClienteCartao;
import com.dimotta.mscartoes.domain.DadosSolicitacaoEmissaoCartao;
import com.dimotta.mscartoes.infra.repository.CartaoRepository;
import com.dimotta.mscartoes.infra.repository.ClienteCartaoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmissaoCartaoSubscriber {

    private final CartaoRepository cartaoRepository;
    private final ClienteCartaoRepository clienteCartaoRepository;

    @RabbitListener(queues = "${mq.queues.emissao-cartoes}")
    public void receberSolicitacaoEmissao(String payload) {

        try {
            var mapper = new ObjectMapper();
            DadosSolicitacaoEmissaoCartao dados =
            mapper.readValue(payload, DadosSolicitacaoEmissaoCartao.class);

            Cartao cartao = cartaoRepository.findById(
                    dados.getIdCartao()).orElseThrow();
            ClienteCartao clienteCartao = new ClienteCartao();
            clienteCartao.setCartao(cartao);
            clienteCartao.setCpf(dados.getCpf());
            clienteCartao.setLimite(dados.getLimiteLiberado());

            clienteCartaoRepository.save(clienteCartao);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}

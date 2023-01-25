package com.dimotta.msavaliadorcredito.application;

import com.dimotta.msavaliadorcredito.application.ex.DadosClienteNotFoundException;
import com.dimotta.msavaliadorcredito.application.ex.ErroComunicacaoMicroservicesException;
import com.dimotta.msavaliadorcredito.application.ex.ErroSolicitacaoCartaoException;
import com.dimotta.msavaliadorcredito.domain.model.DadosAvaliacao;
import com.dimotta.msavaliadorcredito.domain.model.DadosSolicitacaoEmissaoCartao;
import com.dimotta.msavaliadorcredito.domain.model.ProtocoloSolicitacaoCartao;
import com.dimotta.msavaliadorcredito.domain.model.RetornoAvaliacaoCliente;
import com.dimotta.msavaliadorcredito.domain.model.SituacaoCliente;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("avaliacoes-credito")
@RequiredArgsConstructor
public class AvaliadorCreditoController {

    private final AvaliadorCreditoService avaliadorCreditoService;

    @GetMapping
    public String status() {
        return "ok";
    }

    @GetMapping("/situacao-cliente")
    public ResponseEntity consultaSituacaocliente(
            @RequestParam("cpf") String cpf) {

        try {
            SituacaoCliente situacaoCliente = avaliadorCreditoService.obterSituacaoCliente(cpf);
            return ResponseEntity.ok(situacaoCliente);
        } catch (DadosClienteNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (ErroComunicacaoMicroservicesException e) {
            return ResponseEntity.status(
                    HttpStatus.resolve(e.getStatus())).body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity realizarAvaliacao (@RequestBody DadosAvaliacao dados) {
        try {
            RetornoAvaliacaoCliente retornoAvaliacaoCliente =
                    avaliadorCreditoService.realizarAvaliacao(
                            dados.getCpf(), dados.getRenda());

            return ResponseEntity.ok(retornoAvaliacaoCliente);

        } catch (DadosClienteNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (ErroComunicacaoMicroservicesException e) {
            return ResponseEntity.status(
                    HttpStatus.resolve(e.getStatus())).body(e.getMessage());
        }
    }

    @PostMapping("solicitacoes-cartoes")
    public ResponseEntity solicitarCartao(
            @RequestBody DadosSolicitacaoEmissaoCartao dados) {
        try{
            ProtocoloSolicitacaoCartao protocoloSolicitacaoCartao =
                    avaliadorCreditoService.solicitarEmissaoCartao(dados);
            return ResponseEntity.ok(protocoloSolicitacaoCartao);
        }catch (ErroSolicitacaoCartaoException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}

package com.iexec.core.result;

import com.iexec.common.result.eip712.Eip712Challenge;
import com.iexec.common.result.eip712.Eip712ChallengeUtils;
import com.iexec.core.chain.ChainConfig;
import com.iexec.core.chain.CredentialsService;
import com.iexec.core.feign.ResultRepoClient;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.web3j.crypto.ECKeyPair;

import java.util.Optional;


@Slf4j
@Service
public class ResultService {

    private ChainConfig chainConfig;
    private ResultRepoClient resultRepoClient;
    private CredentialsService credentialsService;

    public ResultService(ChainConfig chainConfig, ResultRepoClient resultRepoClient,
                         CredentialsService credentialsService) {
        this.chainConfig = chainConfig;
        this.resultRepoClient = resultRepoClient;
        this.credentialsService = credentialsService;
    }

    @Retryable(value = FeignException.class)
    public boolean isResultUploaded(String chainTaskId) {
        String resultProxyToken = getResultProxyToken();
        if (resultProxyToken.isEmpty()) {
            log.error("isResultUploaded failed (getResultProxyToken) [chainTaskId:{}]", chainTaskId);
            return false;
        }

        return resultRepoClient.isResultUploaded(resultProxyToken, chainTaskId)
                .getStatusCode()
                .is2xxSuccessful();
    }

    // TODO Move this to common since widely used by all iexec services
    private String getResultProxyToken() {
        Optional<Eip712Challenge> oEip712Challenge = getChallenge();
        if (!oEip712Challenge.isPresent()) {
            return "";
        }

        Eip712Challenge eip712Challenge = oEip712Challenge.get();
        ECKeyPair ecKeyPair = credentialsService.getCredentials().getEcKeyPair();
        String walletAddress = credentialsService.getCredentials().getAddress();

        String signedEip712Challenge = Eip712ChallengeUtils.buildAuthorizationToken(
                eip712Challenge,
                walletAddress,
                ecKeyPair);

        if (signedEip712Challenge.isEmpty()) {
            return "";
        }

        String token = login(signedEip712Challenge);

        if (token.isEmpty()) {
            return "";
        }
        return token;
    }

    private Optional<Eip712Challenge> getChallenge() {
        ResponseEntity<Eip712Challenge> challengeResponse = resultRepoClient.getChallenge(chainConfig.getChainId());

        if (challengeResponse != null && challengeResponse.getStatusCode().is2xxSuccessful()) {
            return Optional.of(challengeResponse.getBody());
        }
        return Optional.empty();
    }

    private String login(String token) {
        ResponseEntity<String> loginResponse = resultRepoClient.login(chainConfig.getChainId(), token);
        if (loginResponse != null && loginResponse.getStatusCode().is2xxSuccessful()) {
            return loginResponse.getBody();
        }
        return "";
    }

    @Recover
    private boolean isResultUploaded(FeignException e, String authorizationToken, String chainTaskId) {
        log.error("Cant check isResultUploaded after multiple retries [chainTaskId:{}]", chainTaskId);
        return false;
    }

}
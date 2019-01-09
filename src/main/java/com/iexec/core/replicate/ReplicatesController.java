package com.iexec.core.replicate;

import com.iexec.common.replicate.ReplicateStatus;
import com.iexec.common.replicate.ReplicateStatusModifier;
import com.iexec.core.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
public class ReplicatesController {

    private ReplicatesService replicatesService;
    private JwtTokenProvider jwtTokenProvider;

    public ReplicatesController(ReplicatesService replicatesService,
                                JwtTokenProvider jwtTokenProvider) {
        this.replicatesService = replicatesService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @RequestMapping(method = RequestMethod.POST, path = "/replicates/{chainTaskId}/updateStatus")
    public ResponseEntity updateReplicateStatus(@PathVariable(name = "chainTaskId") String chainTaskId,
                                                @RequestParam(name = "replicateStatus") ReplicateStatus replicateStatus,
                                                @RequestParam(name = "blockNumber") long blockNumber,
                                                @RequestHeader("Authorization") String bearerToken) {
        String walletAddress = jwtTokenProvider.getWalletAddressFromBearerToken(bearerToken);

        if (walletAddress.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED.value()).build();
        }

        log.info("UpdateReplicateStatus requested [chainTaskId:{}, replicateStatus:{}, walletAddress:{}, blockNumber:{}]",
                chainTaskId, replicateStatus, walletAddress, blockNumber);
        replicatesService.updateReplicateStatus(chainTaskId, walletAddress, replicateStatus, blockNumber, ReplicateStatusModifier.WORKER);
        return ResponseEntity.ok().build();
    }
}

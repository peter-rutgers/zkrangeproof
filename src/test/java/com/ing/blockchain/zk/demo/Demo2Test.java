package com.ing.blockchain.zk.demo;

import com.ing.blockchain.zk.RangeProof;
import com.ing.blockchain.zk.TTPGenerator;
import com.ing.blockchain.zk.dto.BoudotRangeProof;
import com.ing.blockchain.zk.dto.ClosedRange;
import com.ing.blockchain.zk.dto.TTPMessage;
import com.ing.blockchain.zk.ethereum.EthereumClient;
import com.ing.blockchain.zk.util.ExportUtil;
import com.ing.blockchain.zk.util.InputUtils;
import org.junit.Test;

import java.math.BigInteger;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertTrue;

public class Demo2Test {

    @Test
    //@Ignore("Can only be executed when Geth is running")
    public void integrationTest() throws ExecutionException, InterruptedException {

        // --- SETUP ---
        EthereumClient client = EthereumClient.getEthereumClient();

        BigInteger x = BigInteger.valueOf(1996);
        String subject = "0x0000000000000000000000000000000000000001";
        String attribute = "birth year";
        String attester = client.getAddress();
        System.out.println("Deploying validator");
        String validatorAddress = client.deployValidator();

        // --- ATTESTING PARTY ---
        TTPMessage message = TTPGenerator.generateTTPMessage(x);
        byte[] commitmentEVM = ExportUtil.exportForEVM(message.getCommitment());

        // Store commitment in blockchain
        //client.storeSignedCommitment(repoAddress, subject, attribute, commitmentEVM);

        // Store TTP Message in file
        String fileName = Config.getInstance().getProperty("ttpmessage.file.name");
        InputUtils.saveObject(fileName, message);

        // --- PROVER ---

        // Generate proof
        ClosedRange range = ClosedRange.of("1950", "2000");
        BoudotRangeProof rangeProof = RangeProof.calculateRangeProof(message, range);
        byte[] proofEVM = ExportUtil.exportForEVM(rangeProof, message.getCommitment(), range);

        // --- VERIFIER ---

        // Validation in Java
        RangeProof.validateRangeProof(rangeProof, message.getCommitment(), range);

        // Validation in Go
        //proofEVM[proofEVM.length / 2] += 1;
        assertTrue(client.validateLocal(validatorAddress, range.getStart(), range.getEnd(), commitmentEVM, proofEVM));

        // Storing validation result in proof repo
        //client.revealRange(repoAddress, attester, subject, attribute, range.getStart(), range.getEnd(), proofEVM);
    }
}

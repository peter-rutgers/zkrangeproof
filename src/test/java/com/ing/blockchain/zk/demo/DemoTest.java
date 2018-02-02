/*
 * Copyright 2017 ING Bank N.V.
 * This file is part of the go-ethereum library.
 *
 * The go-ethereum library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The go-ethereum library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the go-ethereum library. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.ing.blockchain.zk.demo;

import com.ing.blockchain.zk.RangeProof;
import com.ing.blockchain.zk.TTPGenerator;
import com.ing.blockchain.zk.components.SecretOrderGroupGenerator;
import com.ing.blockchain.zk.dto.BoudotRangeProof;
import com.ing.blockchain.zk.dto.ClosedRange;
import com.ing.blockchain.zk.dto.SecretOrderGroup;
import com.ing.blockchain.zk.dto.TTPMessage;
import com.ing.blockchain.zk.ethereum.EthereumClient;
import com.ing.blockchain.zk.util.ExportUtil;
import com.ing.blockchain.zk.util.InputUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.web3j.abi.datatypes.Address;

import java.math.BigInteger;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertTrue;


public class DemoTest {

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
/*

    @Test
    @Ignore("Can only be executed when modified Geth is running")
    public void integrationTest() {

        // --- SETUP ---
        EthereumClient client = EthereumClient.getEthereumClient();

        BigInteger x = BigInteger.valueOf(1996);
        String subject = "0x0000000000000000000000000000000000000001";
        String attribute = "birth year";
        String attester = client.getAddress();

        //String repoAddress = client.deployProofRepo();

        // --- ATTESTING PARTY ---
        SecretOrderGroup group = new SecretOrderGroupGenerator(256).generate();
        TTPMessage message = TTPGenerator.generateTTPMessage(x, group);
        String commitmentCSV = GovernmentDemo.exportCommitment(message.getCommitment());

        // Store commitment in blockchain
        //client.storeSignedCommitment(repoAddress, subject, attribute, commitmentCSV);

        // Store TTP Message in file
        String fileName = Config.getInstance().getProperty("ttpmessage.file.name");
        InputUtils.saveObject(fileName, message);

        // --- PROVER ---

        // Generate proof
        ClosedRange range = ClosedRange.of("1950", "2000");
        BoudotRangeProof rangeProof = RangeProof.calculateRangeProof(message, range);
        String proofCSV = ProverDemo.exportProof(rangeProof);

        // --- VERIFIER ---

        // Validation in Java
        RangeProof.validateRangeProof(rangeProof, message.getCommitment(), range);
        System.out.println(commitmentCSV);
        System.out.println(proofCSV);
        // Validation in Go
        client.validatePrecompiled(range.getStart(), range.getEnd(), commitmentCSV, proofCSV);

        // Storing validation result in proof repo
        //client.revealRange(repoAddress, attester, subject, attribute, range.getStart(), range.getEnd(), proofCSV);
    }
    */
}

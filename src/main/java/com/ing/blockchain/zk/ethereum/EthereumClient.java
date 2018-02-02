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

package com.ing.blockchain.zk.ethereum;

import com.ing.blockchain.zk.demo.Config;
import com.ing.blockchain.zk.dto.ClosedRange;
import com.ing.blockchain.zk.exception.ZeroKnowledgeException;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class EthereumClient {

    private final Web3j web3j;
    private static final BigInteger GAS_LIMIT = BigInteger.valueOf(99999999);
    private static final BigInteger GAS_PRICE = BigInteger.ONE;

    public EthereumClient(final String ethereumUrl) {
        web3j = Web3j.build(new HttpService(ethereumUrl));
    }

    public static EthereumClient getEthereumClient() {
        final String ethereumUrl = Config.getInstance().getProperty("ethereum.url");
        return new EthereumClient(ethereumUrl);
    }

    public String deployValidator() throws ExecutionException, InterruptedException {
        Credentials credentials = getCredentials();
        System.out.println("Deploying contracts, sender = " + getAddress());
        RangeProofValidator rpv = RangeProofValidator.deploy(web3j, credentials, GAS_PRICE, GAS_LIMIT, BigInteger.ZERO).get();
        System.out.println("Deployed validator = " + rpv.getContractAddress());
        return rpv.getContractAddress();
    }

    public String deployProofRepo() throws ExecutionException, InterruptedException {
        return deployProofRepo(deployValidator());
    }

    public String deployProofRepo(String rpvAddress) {
        Credentials credentials = getCredentials();
        try {
            ProofRepo newRepo = ProofRepo.deploy(web3j, credentials, GAS_PRICE, GAS_LIMIT, BigInteger.ZERO, new Address(rpvAddress)).get();
            System.out.println("Deployed proof repo = " + newRepo.getContractAddress());
            return newRepo.getContractAddress();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean validateLocal(String contractAddress, BigInteger lowerBound, BigInteger upperBound, byte[] commitment, byte[] proof) {

        Credentials credentials = getCredentials();
        RangeProofValidator zkpExample = RangeProofValidator.load(contractAddress, web3j, credentials, GAS_PRICE, GAS_LIMIT);

        try {
            System.out.println("Calling validate(lowerBound, upperBound, commitment, proof) on validator contract.");

            Future<TransactionReceipt> future = zkpExample.validate(new Uint256(lowerBound), new Uint256(upperBound),
                    new DynamicBytes(commitment), new DynamicBytes(proof));

            if (future.get().getGasUsed().compareTo(GAS_LIMIT) < 0) {
                System.out.println("Proof validated successfully in Ethereum! " + future.get().getGasUsed());
                return true;
            } else {
                System.out.println("Proof validation failed in Ethereum");
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Cannot call smart contract: " + e.getMessage());
            return false;
        }
    }

    public void storeSignedCommitment(String repoAddress, String subject, String attribute, byte[] commitment) {
        System.out.println("Attesting party = " + getAddress());

        Credentials credentials = getCredentials();
        ProofRepo proofRepo = ProofRepo.load(repoAddress, web3j, credentials, GAS_PRICE, GAS_LIMIT);

        try {
            if (!proofRepo.isValid()) {
                throw new RuntimeException("No valid proof repo at this address");
            }
            System.out.println("Storing commitment");

            Future<TransactionReceipt> result = proofRepo.storeCommitment(new Address(subject), new Utf8String(attribute),
                    new DynamicBytes(commitment));

            TransactionReceipt tr = result.get();
            System.out.println("Stored commitment in block " + tr.getBlockNumber() + " with gas cost : " + tr.getGasUsed());
        } catch (InterruptedException | ExecutionException | IOException e) {
            e.printStackTrace();
        }
    }

    public void revealRange(String repoAddress, String attester, String subject, String attribute, BigInteger lowerBound, BigInteger upperBound, byte[] proof) {
        Credentials credentials = getCredentials();
        ProofRepo proofRepo = ProofRepo.load(repoAddress, web3j, credentials, GAS_PRICE, GAS_LIMIT);

        try {
            if (!proofRepo.isValid()) {
                throw new RuntimeException("No valid proof repo at this address");
            }
            if (!checkExistingCommitment(proofRepo, attester, subject, attribute)) {
                throw new RuntimeException("No signed commitment in repo; check the attributes");
            }

            System.out.println("Storing proof");
            System.out.println("The public range was : " + getRange(proofRepo, attester, subject, attribute));

            Future<TransactionReceipt> result = proofRepo.revealRange(new Address(attester), new Address(subject),
                    new Utf8String(attribute), new Uint256(lowerBound), new Uint256(upperBound), new DynamicBytes(proof));

            TransactionReceipt tr = result.get();
            System.out.println("Stored proof in block " + tr.getBlockNumber() + " with gas cost : " + tr.getGasUsed());
            ClosedRange newRange = getRange(proofRepo, attester, subject, attribute);
            System.out.println("The public range is now : " + newRange);
            if (tr.getGasUsed().equals(GAS_LIMIT)) {
                throw new ZeroKnowledgeException("Transaction failed");
            }
        } catch (InterruptedException | ExecutionException | IOException e) {
            e.printStackTrace();
        }
    }

    private boolean checkExistingCommitment(ProofRepo proofRepo, String attester, String subject, String attribute) throws ExecutionException, InterruptedException {
        DynamicBytes result = proofRepo.getCommitment(new Address(attester), new Address(subject),
                new Utf8String(attribute)).get();
        return result.getValue().length > 0;
    }

    private ClosedRange getRange(ProofRepo proofRepo, String attester, String subject, String attribute) {
        try {
            List<Type> x = proofRepo.getRange(new Address(attester), new Address(subject), new Utf8String(attribute)).get();
            Uint256 lower = (Uint256)x.get(0);
            Uint256 upper = (Uint256)x.get(1);
            return ClosedRange.of(lower.getValue(), upper.getValue());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the private key for the Ethereum-account that interacts with the smart-contract.
     * For testing purpose it gets the private key from a config-file.
     * For production the private key should be retrieved from a wallet.
     * @return The credentials based on the private key.
     */
    private Credentials getCredentials() {

        final String privateKey = Config.getInstance().getProperty("private.key");
        return Credentials.create(privateKey);
    }

    public String getAddress() {
        return getCredentials().getAddress();
    }
}

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
import com.ing.blockchain.zk.dto.*;
import com.ing.blockchain.zk.ethereum.EthereumClient;
import com.ing.blockchain.zk.util.BigIntUtil;
import com.ing.blockchain.zk.util.ExportUtil;
import com.ing.blockchain.zk.util.InputUtils;
import org.bouncycastle.util.BigIntegers;

import javax.xml.bind.DatatypeConverter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static java.math.BigInteger.ONE;

public class ProverDemo {

    public static void main(String args[]) {

        new ProverDemo().runValidation(false);
    }

    public void runValidation(final boolean runLocalValidation) {

        ClosedRange range = InputUtils.readRange(new Scanner(System.in));

        System.out.println("Reading commitment from trusted 3rd party");

        String fileName =Config.getInstance().getProperty("ttpmessage.file.name");
        TTPMessage ttpMessage = (TTPMessage) InputUtils.readObject(fileName);
        Commitment commitment = ttpMessage.getCommitment();

        if (!range.contains(ttpMessage.getX())) {
            throw new IllegalArgumentException("Provided range does not contain the committed value");
        }

        BoudotRangeProof rangeProof = RangeProof.calculateRangeProof(ttpMessage, range);
        InputUtils.saveObject("src/main/resources/range-proof.data", rangeProof);

        //BoudotRangeProof rangeProof = (BoudotRangeProof)InputUtils.readObject("src/main/resources/range-proof.data");

        System.out.println("Commitment = ");
        System.out.println(DatatypeConverter.printHexBinary(ExportUtil.exportForEVM(commitment)));
        //System.out.println(GovernmentDemo.exportCommitment(commitment));

        System.out.println("Proof = ");
        System.out.println(DatatypeConverter.printHexBinary(ExportUtil.exportForEVM(rangeProof, commitment, range)));
        //System.out.println(ProverDemo.exportProof(rangeProof));


        if (runLocalValidation) {
            validateRangeProofLocal(rangeProof, commitment, range);
        } else {
            validateRangeProofOnEthereum(rangeProof, commitment, range);
        }
    }

    void validateRangeProofLocal(BoudotRangeProof rangeProof, Commitment commitment, ClosedRange range) {

        try {
            RangeProof.validateRangeProof(rangeProof, commitment, range);
            System.out.println("Range proof validated successfully");
        } catch (Exception e) {
            System.err.println("Range proof validation error: " + e.getMessage());
            throw e;
            //System.exit(1);
        }
    }

    void validateRangeProofOnEthereum(BoudotRangeProof rangeProof, Commitment commitment, ClosedRange range) {

        EthereumClient client = EthereumClient.getEthereumClient();
        final String contractAddress = Config.getInstance().getProperty("contract.address");

        boolean success = client.validateLocal(contractAddress, range.getStart(), range.getEnd(),
                ExportUtil.exportForEVM(commitment), ExportUtil.exportForEVM(rangeProof, commitment, range));

        if (success) {
            /*
            String repoAddress = Config.getInstance().getProperty("repo.address");
            String attester = InputUtils.readString(new Scanner(System.in), "Attesting party");
            String subject = InputUtils.readString(new Scanner(System.in), "Subject (e.g. 0x0000000000000000000000000000000000000001)");
            String attribute = InputUtils.readString(new Scanner(System.in), "Attribute ");

            client.revealRange(repoAddress, attester, subject, attribute, range.getStart(), range.getEnd(),
                    ExportUtil.exportForEVM(rangeProof, commitment, range));
                    */
        } else {
            System.err.println("Range proof validation failed");
        }

    }

/*
    static String exportProof(BoudotRangeProof p) {
        List<BigInteger> ints = new ArrayList<>();
        ints.add(p.getCLeftSquare());
        ints.add(p.getCRightSquare());
        ints.add(p.getSqrProofLeft().getF());
        ints.add(p.getSqrProofLeft().getECProof().getC());
        ints.add(p.getSqrProofLeft().getECProof().getD());
        ints.add(p.getSqrProofLeft().getECProof().getD1());
        ints.add(p.getSqrProofLeft().getECProof().getD2());
        ints.add(p.getSqrProofRight().getF());
        ints.add(p.getSqrProofRight().getECProof().getC());
        ints.add(p.getSqrProofRight().getECProof().getD());
        ints.add(p.getSqrProofRight().getECProof().getD1());
        ints.add(p.getSqrProofRight().getECProof().getD2());
        ints.add(p.getCftProofLeft().getC());
        ints.add(p.getCftProofLeft().getD1());
        ints.add(p.getCftProofLeft().getD2());
        ints.add(p.getCftProofRight().getC());
        ints.add(p.getCftProofRight().getD1());
        ints.add(p.getCftProofRight().getD2());
        return toCSV(ints);
    }


    static String toCSV(List<BigInteger> ints) {
        int bitlength = 0;
        StringBuilder proofString = new StringBuilder();
        for (int i = 0; i < ints.size(); i++) {
            if (i > 0) {
                proofString.append(',');
            }
            bitlength += ints.get(i).bitLength();
            proofString.append(ints.get(i).toString());
        }
        System.out.println("Range Proof bitlength = " + bitlength);
        return proofString.toString();
    }
    */
}
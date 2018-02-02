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
import com.ing.blockchain.zk.util.ExportUtil;
import com.ing.blockchain.zk.util.InputUtils;

import javax.xml.bind.DatatypeConverter;
import java.math.BigInteger;
import java.util.Scanner;

public class UPortProver {

    public static void main(String args[]) {
        new UPortProver().runValidation();
    }

    public void runValidation() {
        try (Scanner s = new Scanner(System.in)) {
            String[] csv = InputUtils.readString(s, "Enter the range and TTPMessage").split(",");

            if (csv.length != 8) {
                throw new IllegalArgumentException("Input should be CSV of 8 values: lower,upper,<TTP message>");
            }

            TTPMessage ttpMessage = parseTTPMessage(csv);
            ClosedRange range = ClosedRange.of(csv[0], csv[1]);

            if (!range.contains(ttpMessage.getX())) {
                throw new IllegalArgumentException("Provided range does not contain the committed value");
            }

            BoudotRangeProof rangeProof = RangeProof.calculateRangeProof(ttpMessage, range);
            RangeProof.validateRangeProof(rangeProof, ttpMessage.getCommitment(), range);

            System.out.println("Proof = ");
            System.out.println(DatatypeConverter.printHexBinary(ExportUtil.exportForEVM(rangeProof, ttpMessage.getCommitment(), range)));
            //System.out.println(ExportUtil.exportForEVM(rangeProof));
        }
    }

    private TTPMessage parseTTPMessage(String[] fields) {
        SecretOrderGroup group = new SecretOrderGroup(
                new BigInteger(fields[3]),
                new BigInteger(fields[4]),
                new BigInteger(fields[5]));
        Commitment c = new Commitment(group, new BigInteger(fields[2]));
        return new TTPMessage(c, new BigInteger(fields[6]), new BigInteger(fields[7]));
    }

}

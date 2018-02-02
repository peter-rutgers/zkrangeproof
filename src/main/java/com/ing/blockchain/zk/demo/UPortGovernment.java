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

import com.ing.blockchain.zk.components.SecretOrderGroupGenerator;
import com.ing.blockchain.zk.TTPGenerator;
import com.ing.blockchain.zk.dto.SecretOrderGroup;
import com.ing.blockchain.zk.dto.TTPMessage;
import com.ing.blockchain.zk.util.ExportUtil;
import com.ing.blockchain.zk.util.InputUtils;

import javax.xml.bind.DatatypeConverter;
import java.math.BigInteger;
import java.util.Scanner;

public class UPortGovernment {

    public static void main(String[] args) {
        new UPortGovernment().generateTrustedMessage();
    }

    public void generateTrustedMessage() {
        try (Scanner s = new Scanner(System.in)) {
            BigInteger x = InputUtils.readBigInteger(s, "Enter the secret value");
            SecretOrderGroup group = new SecretOrderGroupGenerator().generate();
            TTPMessage message = TTPGenerator.generateTTPMessage(x, group);

            String commitment = DatatypeConverter.printHexBinary(ExportUtil.exportForEVM(message.getCommitment()));
            System.out.println("Commitment (4 public values) =");
            System.out.println(commitment);
            System.out.println("TTP Message (4 public + 2 private values) =");
            System.out.println(commitment + "," + message.getX() + "," + message.getY());
        }
    }
}

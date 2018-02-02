solc --overwrite --abi --bin -o ./solidity/output ./solidity/ProofRepo.sol
#solc --overwrite --abi --bin -o ./solidity/output ./solidity/PrecompiledRangeProof.sol
solc --overwrite --abi --bin -o ./solidity/output ./solidity/RangeProofValidator.sol
./scripts/web3j-2.3.1/bin/web3j solidity generate ./solidity/output/ProofRepo.bin ./solidity/output/ProofRepo.abi -o ./src/main/java -p com.ing.blockchain.zk.ethereum
#./scripts/web3j-2.3.1/bin/web3j solidity generate ./solidity/output/PrecompiledRangeProof.bin ./solidity/output/PrecompiledRangeProof.abi -o ./src/main/java -p com.ing.blockchain.zk.ethereum
./scripts/web3j-2.3.1/bin/web3j solidity generate ./solidity/output/RangeProofValidator.bin ./solidity/output/RangeProofValidator.abi -o ./src/main/java -p com.ing.blockchain.zk.ethereum

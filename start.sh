touch /tmp/empty
~/git/zkrangeproof/go-ethereum/build/bin/geth --datadir ~/ethereum/data --mine --networkid 15997 --nodiscover  --rpc --rpcaddr 0.0.0.0 --rpcport 8545 --rpccorsdomain "*" --targetgaslimit 900000000 --unlock 0 --password /tmp/empty --rpcapi "eth,net,web3,debug" console

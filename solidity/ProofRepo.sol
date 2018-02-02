pragma solidity ^0.4.0;

contract RangeProofValidator {
	function validate(uint lower, uint upper, bytes commitment, bytes proof) constant returns (bool);
}

contract ProofRepo {

    mapping (bytes32 => bytes) signedCommitments;
    mapping (bytes32 => uint256) provenMinimum;
    mapping (bytes32 => uint256) provenMaximum;

    RangeProofValidator rangeProof;

    function ProofRepo(address rpv) {
        rangeProof = RangeProofValidator(rpv);
    }

    function getCommitment(address attester, address subject, string attribute) constant public returns (bytes) {
        bytes32 index = sha256(attester, subject, attribute);
        return signedCommitments[index];
    }

    function storeCommitment(address subject, string attribute, bytes commitment) public {
        bytes32 index = sha256(msg.sender, subject, attribute);
        signedCommitments[index] = commitment;
        provenMinimum[index] = 0;
        provenMaximum[index] = uint256(0) - 1;
    }

    function revealRange(address attester, address subject, string attribute, uint256 lower, uint256 upper, bytes proof) public {
        bytes32 index = sha256(attester, subject, attribute);

        if ( rangeProof.validate(lower, upper, signedCommitments[index], proof)) {
            if (lower > provenMinimum[index])
                provenMinimum[index] = lower;
            if (upper < provenMaximum[index])
                provenMaximum[index] = upper;
        }
    }

    function getRange(address attester, address subject, string attribute) constant public returns (uint256, uint256) {
        bytes32 index = sha256(attester, subject, attribute);

        bool empty = bytes(signedCommitments[index]).length == 0;
        if (empty) return (0, uint256(0) - 1);

        return (provenMinimum[index], provenMaximum[index]);
    }
}

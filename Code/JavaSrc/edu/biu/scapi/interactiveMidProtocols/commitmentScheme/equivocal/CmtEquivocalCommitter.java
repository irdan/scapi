/**
 * 
 */
package edu.biu.scapi.interactiveMidProtocols.commitmentScheme.equivocal;

import java.io.IOException;

import edu.biu.scapi.comm.Channel;
import edu.biu.scapi.exceptions.CheatAttemptException;
import edu.biu.scapi.exceptions.CommitValueException;
import edu.biu.scapi.interactiveMidProtocols.commitmentScheme.CmtCommitter;
import edu.biu.scapi.interactiveMidProtocols.commitmentScheme.CmtWithProofsCommitter;
import edu.biu.scapi.interactiveMidProtocols.commitmentScheme.CmtCommitValue;
import edu.biu.scapi.interactiveMidProtocols.commitmentScheme.CmtCommitmentPhaseValues;
import edu.biu.scapi.interactiveMidProtocols.commitmentScheme.pedersen.CmtPedersenWithProofsCommitter;
import edu.biu.scapi.securityLevel.EquivocalCT;

/**
 * Concrete implementation of Equivocal commitment scheme in the committer's point of view.
 * This is a protocol to obtain an equivocal commitment from any commitment with a ZK-protocol 
 * of the commitment value.
 * The equivocality property means that a simulator can decommit to any value it needs 
 * (needed for proofs of security).
 * 
 * @author Cryptography and Computer Security Research Group Department of Computer Science Bar-Ilan University (Moriya Farbstein)
 *
 */
public class CmtEquivocalCommitter implements CmtCommitter, EquivocalCT{
	
	/*
	  Runs the following pseudo code:
	  	Commit phase
			RUN any COMMIT protocol for C to commit to x
		Decommit phase, using ZK protocol of decommitment value
			SEND x to R
			Run ZK protocol as the prover, that x is the correct decommitment value
	 */
	
	protected CmtWithProofsCommitter committer;
	
	/**
	 * Constructor that gets committer to use in the protocol execution.
	 * @param committer instance of committer that has proofs.
	 */
	public CmtEquivocalCommitter(CmtWithProofsCommitter committer){
		this.committer = committer;
	}
	
	/**
	 * Constructor that gets channel to use in the protocol execution and chooses default committer.
	 * @param channel
	 * @throws CheatAttemptException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public CmtEquivocalCommitter(Channel channel) throws ClassNotFoundException, IOException, CheatAttemptException{
		committer = new CmtPedersenWithProofsCommitter(channel);
	}

	/**
	 * Runs the following line of the protocol:
	 * "RUN any COMMIT protocol for C to commit to x".
	 */
	public void commit(CmtCommitValue input, long id) throws IOException {
		//Delegate to the underlying committer.
		committer.commit(input, id);
	}

	/**
	 * Runs the following lines of the protocol:
	 * "SEND x to R
	 *	Run ZK protocol as the prover, that x is the correct decommitment value".
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws CheatAttemptException 
	 * @throws CommitValueException 
	 */
	public void decommit(long id) throws IOException, CheatAttemptException, ClassNotFoundException, CommitValueException {
		//During the execution of proveCommittedValue, the x is sent to the receiver.
		committer.proveCommittedValue(id);
	}
	
	/**
	 * This function samples random commit value and returns it.
	 * @return the sampled commit value
	 */
	public CmtCommitValue sampleRandomCommitValue(){
		//Delegate to the underlying committer.
		return committer.sampleRandomCommitValue();
	}

	/**
	 * Generates CommitValue from the given byte array.
	 */
	public CmtCommitValue generateCommitValue(byte[] x)
			throws CommitValueException {
		//Delegate to the underlying committer.
		return committer.generateCommitValue(x);
	}
	
	@Override
	public Object[] getPreProcessValues() {
		//Delegate to the underlying committer.
		return committer.getPreProcessValues();
	}

	@Override
	public CmtCommitmentPhaseValues getCommitmentPhaseValues(long id) {
		//Delegate to the underlying committer.
		return committer.getCommitmentPhaseValues(id);
	}
	
	/**
	 * This function converts the given commit value to a byte array. 
	 * @param value
	 * @return the generated bytes.
	 */
	public byte[] generateBytesFromCommitValue(CmtCommitValue value){
		//Delegate to the underlying committer.
		return committer.generateBytesFromCommitValue(value);
	}

}
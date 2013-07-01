/**
* %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
* 
* Copyright (c) 2012 - SCAPI (http://crypto.biu.ac.il/scapi)
* This file is part of the SCAPI project.
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
* 
* Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
* to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
* and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
* 
* The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
* FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
* WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* We request that any publication and/or code referring to and/or based on SCAPI contain an appropriate citation to SCAPI, including a reference to
* http://crypto.biu.ac.il/SCAPI.
* 
* SCAPI uses Crypto++, Miracl, NTL and Bouncy Castle. Please see these projects for any further licensing issues.
* %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
* 
*/
package edu.biu.scapi.interactiveMidProtocols.SigmaProtocol.dh;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

import org.bouncycastle.util.BigIntegers;

import edu.biu.scapi.exceptions.CheatAttemptException;
import edu.biu.scapi.interactiveMidProtocols.SigmaProtocol.SigmaSimulator;
import edu.biu.scapi.interactiveMidProtocols.SigmaProtocol.utility.SigmaBIMsg;
import edu.biu.scapi.interactiveMidProtocols.SigmaProtocol.utility.SigmaProtocolInput;
import edu.biu.scapi.interactiveMidProtocols.SigmaProtocol.utility.SigmaSimulatorOutput;
import edu.biu.scapi.primitives.dlog.DlogGroup;
import edu.biu.scapi.primitives.dlog.GroupElement;
import edu.biu.scapi.primitives.dlog.cryptopp.CryptoPpDlogZpSafePrime;
import edu.biu.scapi.primitives.dlog.miracl.MiraclDlogECF2m;

/**
 * Concrete implementation of Sigma Simulator.
 * This implementation simulates the case that the prover convince a verifier that the input tuple (g,h,u,v) 
 * is a Diffie-Hellman tuple.
 * 
 * @author Cryptography and Computer Security Research Group Department of Computer Science Bar-Ilan University (Moriya Farbstein)
 *
 */
public class SigmaDHSimulator implements SigmaSimulator{
	/*	
	  This class computes the following calculations:
		  	SAMPLE a random z <- Zq
			COMPUTE a = g^z*u^(-e) and b = h^0z*v^(-e) (where �e here means �e mod q)
			OUTPUT ((a,b),e,z)
	
	*/

	private DlogGroup dlog; 		//Underlying DlogGroup.
	private int t;					//Soundness parameter.
	private SecureRandom random;
	private BigInteger qMinusOne;
	
	/**
	 * Constructor that gets the underlying DlogGroup, soundness parameter and SecureRandom.
	 * @param dlog
	 * @param t Soundness parameter in BITS.
	 * @param random
	 */
	public SigmaDHSimulator(DlogGroup dlog, int t, SecureRandom random){
		// Sets the given parameters.
		setParameters(dlog, t, random);
	}
	
	/**
	 * Default constructor that chooses default values for the parameters.
	 */
	public SigmaDHSimulator() {
		try {
			//Calls the other constructor with Miracl Koblitz 233 Elliptic curve.
			setParameters(new MiraclDlogECF2m("K-233"), 80, new SecureRandom());
		} catch (IOException e) {
			//If there is a problem with the elliptic curves file, create Zp DlogGroup.
			setParameters(new CryptoPpDlogZpSafePrime(), 80, new SecureRandom());
		}
	}

	/**
	 * If soundness parameter is valid, sets the parameters. Else, throw IllegalArgumentException.
	 * @param dlog
	 * @param t soundness parameter in BITS
	 * @param random
	 * @throws IllegalArgumentException if soundness parameter is invalid.
	 */
	private void setParameters(DlogGroup dlog, int t, SecureRandom random) {
		
		//Sets the parameters.
		this.dlog = dlog;
		this.t = t;
		
		//Check the soundness validity.
		if (!checkSoundness()){
			throw new IllegalArgumentException("soundness parameter t does not satisfy 2^t<q");
		}
		
		this.random = random;
		qMinusOne = dlog.getOrder().subtract(BigInteger.ONE);
	}
	
	/**
	 * Checks the validity of the given soundness parameter.
	 * @return true if the soundness parameter is valid; false, otherwise.
	 */
	private boolean checkSoundness(){
		//If soundness parameter does not satisfy 2^t<q, return false.
		BigInteger soundness = new BigInteger("2").pow(t);
		BigInteger q = dlog.getOrder();
		if (soundness.compareTo(q) >= 0){
			return false;
		}
		return true;
	}
	
	/**
	 * Returns the soundness parameter for this Sigma protocol.
	 * @return t soundness parameter
	 */
	public int getSoundness(){
		return t;
	}
	
	/**
	 * Computes the simulator computation.
	 * @param input MUST be an instance of SigmaDHInput.
	 * @param challenge
	 * @return the output of the computation - (a, e, z).
	 * @throws CheatAttemptException if the received challenge's length is not equal to the soundness parameter.
	 * @throws IllegalArgumentException if the given input is not an instance of SigmaDHInput.
	 */
	public SigmaSimulatorOutput simulate(SigmaProtocolInput input, byte[] challenge) throws CheatAttemptException{
		//check the challenge validity.
		if (!checkChallengeLength(challenge)){
			throw new CheatAttemptException("the length of the given challenge is differ from the soundness parameter");
		}
		if (!(input instanceof SigmaDHInput)){
			throw new IllegalArgumentException("the given input must be an instance of SigmaDHInput");
		}
		SigmaDHInput dhInput = (SigmaDHInput) input;
		
		//Sample a random z <- Zq
		BigInteger z = BigIntegers.createRandomInRange(BigInteger.ZERO, qMinusOne, random);
		
		//Compute a = g^z*u^(-e) (where �e here means �e mod q)
		GroupElement gToZ = dlog.exponentiate(dlog.getGenerator(), z);
		BigInteger e = new BigInteger(1, challenge);
		BigInteger minusE = dlog.getOrder().subtract(e);
		GroupElement uToE = dlog.exponentiate(dhInput.getU(), minusE);
		GroupElement a = dlog.multiplyGroupElements(gToZ, uToE);
		
		//Compute b = h^z*v^(-e) (where �e here means �e mod q)
		GroupElement hToZ = dlog.exponentiate(dhInput.getH(), z);
		GroupElement vToE = dlog.exponentiate(dhInput.getV(), minusE);
		GroupElement b = dlog.multiplyGroupElements(hToZ, vToE);
		
		//Output ((a,b),e,z).
		return new SigmaDHSimulatorOutput(new SigmaDHMsg(a.generateSendableData(), b.generateSendableData()), challenge, new SigmaBIMsg(z));
				
	}
	
	/**
	 * Computes the simulator computation.
	 * @param input MUST be an instance of SigmaDlogInput.
	 * @return the output of the computation - (a, e, z).
	 * @throws IllegalArgumentException if the given input is not an instance of SigmaDlogInput.
	 */
	public SigmaSimulatorOutput simulate(SigmaProtocolInput input){
		//Create a new byte array of size t/8, to get the required byte size.
		byte[] e = new byte[t/8];
		//Fill the byte array with random values.
		random.nextBytes(e);
		//Call the other simulate function with the given input and the samples e.
		try {
			return simulate(input, e);
		} catch (CheatAttemptException e1) {
			//will not occur since the challenge length is valid.
		}
		return null;
	}
	
	/**
	 * Checks if the given challenge length is equal to the soundness parameter.
	 * @return true if the challenge length is t; false, otherwise. 
	 */
	private boolean checkChallengeLength(byte[] challenge){
		//If the challenge's length is equal to t, return true. else, return false.
		return (challenge.length == (t/8) ? true : false);
	}
}

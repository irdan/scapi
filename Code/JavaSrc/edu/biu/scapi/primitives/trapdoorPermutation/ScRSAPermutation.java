package edu.biu.scapi.primitives.trapdoorPermutation;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.logging.Level;

import edu.biu.scapi.exceptions.UnInitializedException;
import edu.biu.scapi.generals.Logging;

/**
 * Concrete class of trapdoor permutation for RSA.
 * 
 * @author Cryptography and Computer Security Research Group Department of Computer Science Bar-Ilan University (Moriya Farbstein)
 *
 */
public final class ScRSAPermutation extends TrapdoorPermutationAbs implements RSAPermutation {
	
	
	/** 
	 * No such initialization for RSA permutation.
	 * This RSA implementation can be initialized by two ways:
	 * 1. keys
	 * 2. algorithmParameterSpec
	 * any combination of these ways is not a legal initialization.
	 * @throws UnsupportedOperationException 
	 */
	public void init(PublicKey publicKey, PrivateKey privateKey,
			AlgorithmParameterSpec params) throws UnsupportedOperationException {
		/*initialization of RSA can be done by two ways:
		 * 1. keys
		 * 2. algorithmParameterSpec
		 * any combination of these ways is not a legal initialization.
		 */
		throw new UnsupportedOperationException("no such RSA initialization");
	}

	/** 
	 * Initializes this RSA permutation with keys
	 * @param publicKey - public key
	 * @param privateKey - private key
	 * @throws InvalidKeyException if the keys are not RSA keys
	 */
	public void init(PublicKey publicKey, PrivateKey privateKey) throws InvalidKeyException {
		
		if (!(publicKey instanceof RSAPublicKey) || !(privateKey instanceof RSAPrivateKey)) {
			throw new InvalidKeyException("Key type doesn't match the trapdoor permutation type");
		}
		
		modN = ((RSAPublicKey)publicKey).getModulus();
			
		//calls the father init that sets the keys
		super.init(publicKey, privateKey);
			
	}
	
	/** 
	 * Initializes this RSA permutation with public key.
	 * After this initialization, this object can do compute but not invert.
	 * This initialization is for user that wants to encrypt a message using the public key but deosn't want to decrypt a message.
	 * @param publicKey - public key
	 * @throws InvalidKeyException if the key is not a RSA key
	 */
	public void init(PublicKey publicKey) throws InvalidKeyException {
		
		if (!(publicKey instanceof RSAPublicKey)) {
			throw new InvalidKeyException("Key type doesn't match the trapdoor permutation type");
		}
			
		modN = ((RSAPublicKey)publicKey).getModulus();
		
		//calls the father init that sets the key
		super.init(publicKey);
			
		
	}
	
	/** 
	 * Initializes this RSA permutation with params.
	 * @param params auxiliary parameters
	 * @throws InvalidParameterSpecException if params are not RSA parameter spec
	 */
	public void init(AlgorithmParameterSpec params) throws InvalidParameterSpecException {
		
		if(!(params instanceof RSAKeyGenParameterSpec)) {
			throw new InvalidParameterSpecException("AlgorithmParameterSpec type doesn't match the trapdoor permutation type");
		}
	
		try {
			/*generates public and private keys */
			KeyPairGenerator kpr;
			kpr = KeyPairGenerator.getInstance("RSA");
			kpr.initialize(((RSAKeyGenParameterSpec) params).getKeysize());
			KeyPair pair = kpr.generateKeyPair();
			PublicKey publicKey = pair.getPublic();
			PrivateKey privateKey = pair.getPrivate();
			
			//init the trapdoor permutation with this keys
			init(publicKey, privateKey);
			
			//calls the parent init
			super.init(params);
		} catch (NoSuchAlgorithmException e) {
			Logging.getLogger().log(Level.WARNING, e.toString());
		} catch (InvalidKeyException e) {
			Logging.getLogger().log(Level.WARNING, e.toString());
		}
	}
	
	/** 
	 * @return the algorithm name - "RSA"
	 */
	public String getAlgorithmName() {
		return "RSA";
	}
	
	/** 
	 * Computes the  RSA permutation on the given TPElement 
	 * @param tpEl - the input for the computation
	 * @return - the result TPElement
	 * @throws UnInitializedException if this object is not initialized
	 * @throw IllegalArgumentException if the given element is not a RSA element
	 */
	public TPElement compute(TPElement tpEl) throws IllegalArgumentException, UnInitializedException{
		if (!IsInitialized()){
			throw new UnInitializedException();
		}
		
		if (!(tpEl instanceof RSAElement)) {
			throw new IllegalArgumentException("trapdoor element doesn't match the trapdoor permutation");
		}
		
		// gets the value of the element 
		BigInteger element = ((RSAElement)tpEl).getElement();
		//compute - calculates (element^e)modN
		BigInteger result = element.modPow(
        		((RSAPublicKey)pubKey).getPublicExponent(), ((RSAPublicKey)pubKey).getModulus());
		// builds the return element
		RSAElement returnEl = new RSAElement(modN, result);			
		//returns the result of the computation
		return returnEl;
	}

	/** 
	 * Inverts the RSA permutation on the given TPElement.
	 * @param tpEl - the input to invert
	 * @return - the result 
	 * @throws IllegalArgumentException if the given element is not a RSA element
	 * @throws UnInitializedException if this object is not initialized
	 */
	public TPElement invert(TPElement tpEl)  throws IllegalArgumentException, UnInitializedException{
		if (!IsInitialized()){
			throw new UnInitializedException();
		}
		//in case that the initialization was with public key and no privte key- can't do the invert and returns null
		if (privKey == null && pubKey!=null)
			return null;
		
		if (!(tpEl instanceof RSAElement)) {
			throw new IllegalArgumentException("trapdoor element doesn't match the trapdoor permutation");
		}
		
		// gets the value of the element 
		BigInteger element = ((RSAElement)tpEl).getElement();
		//invert the permutation
		BigInteger result = doInvert(element);
		//builds the return element
		RSAElement returnEl = new RSAElement(modN, result);
		//returns the result
		return returnEl;
	}

	/**
	 * Inverts the permutation according to the RSA key.
	 * If the key is CRT key - invert using the Chinese Remainder Theorem.
	 * Else - invert using d, modN.
	 * @param input - The element to invert
	 * @return BigInteger - the result
	 */
	private BigInteger doInvert(BigInteger input)
    {
		if (privKey instanceof RSAPrivateCrtKey) //invert with CRT parameters
        {
            // we have the extra factors, use the Chinese Remainder Theorem 
            RSAPrivateCrtKey crtKey = (RSAPrivateCrtKey)privKey;

            //gets the crt parameters
            BigInteger p = crtKey.getPrimeP();
            BigInteger q = crtKey.getPrimeQ();
            BigInteger dP = crtKey.getPrimeExponentP();
            BigInteger dQ = crtKey.getPrimeExponentQ();
            BigInteger qInv = crtKey.getCrtCoefficient();

            BigInteger mP, mQ, h, m;

            // mP = ((input mod p) ^ dP)) mod p
            mP = (input.remainder(p)).modPow(dP, p);

            // mQ = ((input mod q) ^ dQ)) mod q
            mQ = (input.remainder(q)).modPow(dQ, q);

            // h = qInv * (mP - mQ) mod p
            h = mP.subtract(mQ);
            h = h.multiply(qInv);
            h = h.mod(p);               // mod returns the positive residual

            // m = h * q + mQ
            m = h.multiply(q);
            m = m.add(mQ);

            return m;
        }
        else{//invert using d, modN
            return input.modPow(
            		((RSAPrivateKey)privKey).getPrivateExponent(), ((RSAPrivateKey)pubKey).getModulus());
        }
    }
	
	
	/** 
	 * Checks if the given element is valid to RSA permutation
	 * @param tpEl - the element to check
	 * @return TPElValidity - enum number that indicate the validation of the element 
	 * There are three possible validity values: 
	 * VALID (it is an element)
	 * NOT_VALID (it is not an element)
	 * DON�T_KNOW (there is not enough information to check if it is an element or not)  
	 * @throws IllegalArgumentException if the given element is not a RSA element
	 * @throws UnInitializedException if this object is not initialized
	 */
	public TPElValidity isElement(TPElement tpEl) throws IllegalArgumentException, UnInitializedException{
		
		if (!IsInitialized()){
			throw new UnInitializedException();
		}
		if (!(tpEl instanceof RSAElement)){
			throw new IllegalArgumentException("trapdoor element doesn't match the trapdoor permutation");
		}
			
		TPElValidity validity = null;
		BigInteger value = ((RSAElement)tpEl).getElement();
		
		//if mod n is unknown - returns DONT_KNOW 
		if (modN==null) {
			validity = TPElValidity.DONT_KNOW;
			
		//if the value is valid (between 1 to (mod n) - 1) returns VALID 
		} else if(((value.compareTo(BigInteger.ZERO))>0) && (value.compareTo(modN)<0)) {
			
			validity = TPElValidity.VALID;
		//if the value is invalid returns NOT_VALID 
		} else {
			validity = TPElValidity.NOT_VALID;
		}		
		
		//returns the correct TPElValidity
		return validity;
	}

	/** 
	 * creates a random RSAElement 
	 * @return TPElement - the created RSA element
	 * @throws UnInitializedException if this object is not initialized
	 */
	public TPElement getRandomTPElement() throws UnInitializedException {
		if (!IsInitialized()){
			throw new UnInitializedException();
		}
		return new RSAElement(modN);
	}


}
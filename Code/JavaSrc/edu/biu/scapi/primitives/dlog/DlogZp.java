package edu.biu.scapi.primitives.dlog;

import java.math.BigInteger;

import edu.biu.scapi.primitives.dlog.groupParams.ZpGroupParams;

/**
 * Marker interface. Every class that implements it is signed as Zp*
 * @author Moriya
 *
 */
public interface DlogZp extends DlogGroup{

	/**
	 * Initialize the CryptoPP implementation of Dlog over Zp* 
	 * @param groupParams
	 */
	public void init(ZpGroupParams groupParams);
	
	/**
	 * Creates an element with the given x value 
	 * @param x
	 * @return the created element
	 */
	public ZpElement getElement (BigInteger x);
}

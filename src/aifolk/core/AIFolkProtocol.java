package aifolk.core;

/**
 * Constants related to the AIFolk interaction protocol.
 */
public class AIFolkProtocol {
	/**
	 * The key indicating the protocol of the message.
	 */
	protected static final String FOLK_PROTOCOL = "protocol";
	/**
	 * The key containing the arguments in the message (depending on protocol).
	 */
	protected static final String	FOLK_ARGUMENTS	= "args";
	/**
	 * SEARCH message
	 */
	protected static final String	FOLK_SEARCH		= "aifolk-search";
	/**
	 * LISTING message
	 */
	protected static final String	FOLK_LISTING	= "aifolk-listing";
	/**
	 * REQUEST message
	 */
	protected static final String	FOLK_REQUEST	= "aifolk-request";
	/**
	 * TRANSFER message
	 */
	protected static final String	FOLK_TRANSFER	= "aifolk-transfer";
}

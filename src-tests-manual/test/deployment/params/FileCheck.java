package test.deployment.params;

import net.xqhs.flash.FlashBoot;

/*
Manual test: verifies graceful failure behavior when deployment or schema
files are missing or invalid.

Two auxiliary files are used:
    invalid-deployment.xml} — a malformed XML deployment file
    invalid-schema.xsd} — a malformed XSD schema file
Expected output for each run: "Deployment file load failed."
*/
public class FileCheck {

	static final String THIS_DIRECTORY = "src-tests/test/deployment/";
	public static void main(String[] args) {
		// scenario 1: deployment file exists but content is malformed XML
		FlashBoot.main(new String[] { THIS_DIRECTORY + "invalid-deployment.xml" });

		// scenario 2: valid deployment file, but schema is invalid
		// -schema <file> overrides the default schema path
		FlashBoot.main(new String[] {
				"src-tests/test/empties/empty-scenario.xml",
				"-schema", THIS_DIRECTORY + "invalid-schema.xsd" });
	}
}

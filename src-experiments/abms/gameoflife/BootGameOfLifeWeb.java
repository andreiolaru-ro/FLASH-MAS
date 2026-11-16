package abms.gameoflife;

import net.xqhs.flash.FlashBoot;

public class BootGameOfLifeWeb {

	public static void main(String[] args) {
		StringBuilder testArgs = new StringBuilder();

		testArgs.append(" -loader agent:composite");
		testArgs.append(" -package abms.gameoflife abms.samples testing net.xqhs.flash.abms");
		testArgs.append(" -node main central:web");
		testArgs.append(" -pylon webSocket:serverPylon serverPort:8886");

		int rows = Integer.getInteger("gol.rows", 20);
		int cols = Integer.getInteger("gol.cols", 40);
		int updateFrequency = Integer.getInteger("gol.updateMs", 300);

		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				testArgs.append(" -agent composite:cell_").append(r).append("_").append(c)
						.append(" -shard messaging")
						.append(" -shard remoteOperation update-frequency:").append(updateFrequency)
						.append(" -shard CellSimulationLogic");
			}
		}

		FlashBoot.main(testArgs.toString().split("\\s+"));
	}
}

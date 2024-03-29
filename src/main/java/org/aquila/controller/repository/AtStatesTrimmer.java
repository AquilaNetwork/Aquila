package org.aquila.controller.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aquila.controller.Controller;
import org.aquila.controller.Synchronizer;
import org.aquila.data.block.BlockData;
import org.aquila.repository.DataException;
import org.aquila.repository.Repository;
import org.aquila.repository.RepositoryManager;
import org.aquila.settings.Settings;
import org.aquila.utils.NTP;

public class AtStatesTrimmer implements Runnable {

	private static final Logger LOGGER = LogManager.getLogger(AtStatesTrimmer.class);

	@Override
	public void run() {
		Thread.currentThread().setName("AT States trimmer");

		if (Settings.getInstance().isLite()) {
			// Nothing to trim in lite mode
			return;
		}

		try (final Repository repository = RepositoryManager.getRepository()) {
			int trimStartHeight = repository.getATRepository().getAtTrimHeight();
			int maxLatestAtStatesHeight = PruneManager.getMaxHeightForLatestAtStates(repository);

			repository.discardChanges();
			repository.getATRepository().rebuildLatestAtStates(maxLatestAtStatesHeight);
			repository.saveChanges();

			while (!Controller.isStopping()) {
				repository.discardChanges();

				Thread.sleep(Settings.getInstance().getAtStatesTrimInterval());

				BlockData chainTip = Controller.getInstance().getChainTip();
				if (chainTip == null || NTP.getTime() == null)
					continue;

				// Don't even attempt if we're mid-sync as our repository requests will be delayed for ages
				if (Synchronizer.getInstance().isSynchronizing())
					continue;

				long currentTrimmableTimestamp = NTP.getTime() - Settings.getInstance().getAtStatesMaxLifetime();
				// We want to keep AT states near the tip of our copy of blockchain so we can process/orphan nearby blocks
				long chainTrimmableTimestamp = chainTip.getTimestamp() - Settings.getInstance().getAtStatesMaxLifetime();

				long upperTrimmableTimestamp = Math.min(currentTrimmableTimestamp, chainTrimmableTimestamp);
				int upperTrimmableHeight = repository.getBlockRepository().getHeightFromTimestamp(upperTrimmableTimestamp);

				int upperBatchHeight = trimStartHeight + Settings.getInstance().getAtStatesTrimBatchSize();
				int upperTrimHeight = Math.min(upperBatchHeight, upperTrimmableHeight);

				if (trimStartHeight >= upperTrimHeight)
					continue;

				int numAtStatesTrimmed = repository.getATRepository().trimAtStates(trimStartHeight, upperTrimHeight, Settings.getInstance().getAtStatesTrimLimit());
				repository.saveChanges();

				if (numAtStatesTrimmed > 0) {
					final int finalTrimStartHeight = trimStartHeight;
					LOGGER.debug(() -> String.format("Trimmed %d AT state%s between blocks %d and %d",
							numAtStatesTrimmed, (numAtStatesTrimmed != 1 ? "s" : ""),
							finalTrimStartHeight, upperTrimHeight));
				} else {
					// Can we move onto next batch?
					if (upperTrimmableHeight > upperBatchHeight) {
						trimStartHeight = upperBatchHeight;
						repository.getATRepository().setAtTrimHeight(trimStartHeight);
						maxLatestAtStatesHeight = PruneManager.getMaxHeightForLatestAtStates(repository);
						repository.getATRepository().rebuildLatestAtStates(maxLatestAtStatesHeight);
						repository.saveChanges();

						final int finalTrimStartHeight = trimStartHeight;
						LOGGER.debug(() -> String.format("Bumping AT state base trim height to %d", finalTrimStartHeight));
					}
				}
			}
		} catch (DataException e) {
			LOGGER.warn(String.format("Repository issue trying to trim AT states: %s", e.getMessage()));
		} catch (InterruptedException e) {
			// Time to exit
		}
	}

}

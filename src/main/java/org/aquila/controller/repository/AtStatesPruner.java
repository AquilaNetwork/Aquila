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

public class AtStatesPruner implements Runnable {

	private static final Logger LOGGER = LogManager.getLogger(AtStatesPruner.class);

	@Override
	public void run() {
		Thread.currentThread().setName("AT States pruner");

		if (Settings.getInstance().isLite()) {
			// Nothing to prune in lite mode
			return;
		}

		boolean archiveMode = false;
		if (!Settings.getInstance().isTopOnly()) {
			// Top-only mode isn't enabled, but we might want to prune for the purposes of archiving
			if (!Settings.getInstance().isArchiveEnabled()) {
				// No pruning or archiving, so we must not prune anything
				return;
			}
			else {
				// We're allowed to prune blocks that have already been archived
				archiveMode = true;
			}
		}

		try (final Repository repository = RepositoryManager.getRepository()) {
			int pruneStartHeight = repository.getATRepository().getAtPruneHeight();
			int maxLatestAtStatesHeight = PruneManager.getMaxHeightForLatestAtStates(repository);

			repository.discardChanges();
			repository.getATRepository().rebuildLatestAtStates(maxLatestAtStatesHeight);
			repository.saveChanges();

			while (!Controller.isStopping()) {
				repository.discardChanges();

				Thread.sleep(Settings.getInstance().getAtStatesPruneInterval());

				BlockData chainTip = Controller.getInstance().getChainTip();
				if (chainTip == null || NTP.getTime() == null)
					continue;

				// Don't even attempt if we're mid-sync as our repository requests will be delayed for ages
				if (Synchronizer.getInstance().isSynchronizing())
					continue;

				// Prune AT states for all blocks up until our latest minus pruneBlockLimit
				final int ourLatestHeight = chainTip.getHeight();
				int upperPrunableHeight = ourLatestHeight - Settings.getInstance().getPruneBlockLimit();

				// In archive mode we are only allowed to trim blocks that have already been archived
				if (archiveMode) {
					upperPrunableHeight = repository.getBlockArchiveRepository().getBlockArchiveHeight() - 1;

					// TODO: validate that the actual archived data exists before pruning it?
				}

				int upperBatchHeight = pruneStartHeight + Settings.getInstance().getAtStatesPruneBatchSize();
				int upperPruneHeight = Math.min(upperBatchHeight, upperPrunableHeight);

				if (pruneStartHeight >= upperPruneHeight)
					continue;

				LOGGER.debug(String.format("Pruning AT states between blocks %d and %d...", pruneStartHeight, upperPruneHeight));

				int numAtStatesPruned = repository.getATRepository().pruneAtStates(pruneStartHeight, upperPruneHeight);
				repository.saveChanges();
				int numAtStateDataRowsTrimmed = repository.getATRepository().trimAtStates(
						pruneStartHeight, upperPruneHeight, Settings.getInstance().getAtStatesTrimLimit());
				repository.saveChanges();

				if (numAtStatesPruned > 0 || numAtStateDataRowsTrimmed > 0) {
					final int finalPruneStartHeight = pruneStartHeight;
					LOGGER.debug(() -> String.format("Pruned %d AT state%s between blocks %d and %d",
							numAtStatesPruned, (numAtStatesPruned != 1 ? "s" : ""),
							finalPruneStartHeight, upperPruneHeight));
				} else {
					// Can we move onto next batch?
					if (upperPrunableHeight > upperBatchHeight) {
						pruneStartHeight = upperBatchHeight;
						repository.getATRepository().setAtPruneHeight(pruneStartHeight);
						maxLatestAtStatesHeight = PruneManager.getMaxHeightForLatestAtStates(repository);
						repository.getATRepository().rebuildLatestAtStates(maxLatestAtStatesHeight);
						repository.saveChanges();

						final int finalPruneStartHeight = pruneStartHeight;
						LOGGER.debug(() -> String.format("Bumping AT state base prune height to %d", finalPruneStartHeight));
					}
					else {
						// We've pruned up to the upper prunable height
						// Back off for a while to save CPU for syncing
						repository.discardChanges();
						Thread.sleep(5*60*1000L);
					}
				}
			}
		} catch (DataException e) {
			LOGGER.warn(String.format("Repository issue trying to prune AT states: %s", e.getMessage()));
		} catch (InterruptedException e) {
			// Time to exit
		}
	}

}

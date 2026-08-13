plugins {
	id("dev.kikugie.stonecutter")
}

// Which version's checked-out sources the IDE edits / git tracks. Switch with the
// "Set active project to <version>" Gradle task (IDE: Gradle tool window > Tasks > stonecutter).
// Always run "Reset active project" before committing - it switches back to this version
// and re-comments the other branches, so `git diff` only ever shows real changes.
stonecutter active "26.1.2"

stonecutter parameters {
	// Per-version code differences live directly in the source as comment directives, e.g.:
	//
	//   //? if <26.2
	//   client.setScreen(screen);
	//   //? if >=26.2
	//   //client.gui.setScreen(screen);
	//
	// or, for multi-line blocks:
	//
	//   //? if <26.2 {
	//   <old code>
	//   //?} else {
	//   /*<new code>
	//   *///?}
	//
	// See the API differences already handled between 26.1.2 and 26.2 in AchievementManager,
	// BuffTracker, CheeseDetector, CheeseHud, RatCheeseMod and the screen/ package.
}

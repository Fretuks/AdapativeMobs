package net.fretux.adaptivemobs.gametest;

import net.fretux.adaptivemobs.ai.goals.AdaptiveSlimeTacticsGoal;
import net.fretux.adaptivemobs.data.TemporaryBlockSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Slime;
import net.minecraftforge.gametest.GameTestHolder;

import java.util.Iterator;

@GameTestHolder("adaptivemobs")
public final class AdaptiveServerBehaviorGameTests {
    private AdaptiveServerBehaviorGameTests() {
    }

    @GameTest(template = "empty")
    public static void temporaryBlocksSurviveSavedDataReload(GameTestHelper helper) {
        TemporaryBlockSavedData original = new TemporaryBlockSavedData();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        long expires = helper.getLevel().getGameTime() + 100L;
        original.add(helper.getLevel(), pos, expires);

        TemporaryBlockSavedData reloaded = TemporaryBlockSavedData.load(original.save(new CompoundTag()));
        Iterator<TemporaryBlockSavedData.Entry> entries = reloaded.entries();
        helper.assertTrue(entries.hasNext(), "temporary block record was lost during save/reload");
        TemporaryBlockSavedData.Entry entry = entries.next();
        helper.assertTrue(entry.dimension().equals(helper.getLevel().dimension().location()), "dimension was not preserved");
        helper.assertTrue(entry.pos().equals(pos), "position was not preserved");
        helper.assertTrue(entry.expireTick() == expires, "expiration was not preserved");
        helper.assertTrue(!entries.hasNext(), "unexpected duplicate temporary block record");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void slimeRecombinationCancelsWhenStuck(GameTestHelper helper) {
        Slime slime = helper.spawn(EntityType.SLIME, new BlockPos(1, 2, 1));
        Slime partner = helper.spawn(EntityType.SLIME, new BlockPos(8, 2, 1));
        slime.setSize(1, true);
        partner.setSize(1, true);
        AdaptiveSlimeTacticsGoal goal = new AdaptiveSlimeTacticsGoal(slime, () -> 5);

        helper.assertTrue(goal.canUse(), "slime did not select its eligible partner");
        goal.start();
        for (int i = 0; i <= 40; i++) {
            goal.tick();
        }
        helper.assertTrue(!goal.canContinueToUse(), "stuck recombination chase did not time out");
        goal.stop();
        helper.succeed();
    }
}

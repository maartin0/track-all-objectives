package me.maartin0.TrackAllObjectives.mixin;

import com.google.common.collect.Lists;
import me.maartin0.TrackAllObjectives.Main;
import net.minecraft.registry.Registries;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stat.Stat;
import net.minecraft.stat.StatType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Optional;

@Mixin(MinecraftServer.class)
public abstract class InitMixin {
    @Shadow public abstract ServerScoreboard getScoreboard();

    @Inject(at = @At("RETURN"), method = "loadWorld")
    private void init(CallbackInfo info) {
        Main.LOGGER.info("Starting objective gen");
        ArrayList<String> list = Lists.newArrayList(ScoreboardCriterion.getAllSimpleCriteria());
        for (StatType statType : Registries.STAT_TYPE) {
            for (Object object : statType.getRegistry()) {
                String string = Stat.getName(statType, object);
                list.add(string);
            }
        }
        Scoreboard scoreboard = getScoreboard();
        long successes = list.stream().filter(criterion -> {
            Optional<ScoreboardCriterion>
                    optionalCriterionObject = ScoreboardCriterion.getOrCreateStatCriterion(criterion);
            if (optionalCriterionObject.isEmpty()) return false;
            ScoreboardCriterion criterionObject = optionalCriterionObject.get();
            try {
                scoreboard.addObjective(
                    criterion.replace(":", "."),
                    criterionObject,
                    Text.of(Main.toDisplayName(criterion)),
                    criterionObject.getDefaultRenderType(),
                    false,
                    null
                );
            } catch (IllegalArgumentException e) {
                return false;
            }
            return true;
        }).count();
        Main.LOGGER.info(String.format("Successfully generated %s/%s objectives. You should now remove this mod's jar from the mods folder.", successes, list.size()));
    }
}

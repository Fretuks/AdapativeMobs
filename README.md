# Adaptive Mobs

Adaptive Mobs is a Minecraft Forge 1.20.1 server-side gameplay mod that makes existing vanilla hostile mobs scale with world progression. It does not add new mobs, variants, models, entity types, or assets.

## What Changes

- Tracks global world progression with saved data.
- Counts total hostile mob kills by players.
- Counts hostile kills per entity type.
- Uses world day plus kill thresholds to compute adaptive tiers 0-5.
- Scales hostile mob attributes on spawn: health, damage, follow range, small speed bonuses, armor, and knockback resistance.
- Improves vanilla equipment for eligible mobs with vanilla items only.
- Adds capped spawn pressure by occasionally spawning one extra vanilla mob of the same type near natural hostile spawns.
- Injects lightweight AI goals for vanilla hostile mobs where practical.
- Adds small tactical responses for group morale, vulnerable target priority, low-health retreats, dark approach paths, and common cheese patterns.
- Does not add new mobs, variants, models, entity types, or assets.
- Does not modify bosses by default.

## Default Tiers

- Tier 0: vanilla behavior.
- Tier 1: day 5 or 25 hostile kills.
- Tier 2: day 15 or 100 hostile kills.
- Tier 3: day 30 or 250 hostile kills.
- Tier 4: day 60 or 600 hostile kills.
- Tier 5: day 100 or 1200 hostile kills.

Mob-specific tiers can rise further when players repeatedly kill the same mob type. The global tier remains the main progression source.

## Tier 5 Defaults

At the highest default tier, eligible hostile mobs receive:

- `+75%` max health.
- `+45%` attack damage when the mob has an attack damage attribute.
- `+5%` movement speed.
- `+25%` follow range.
- `+4` armor.
- `+0.10` knockback resistance.
- A heal to the new maximum health after scaling is applied.

Spawn pressure is also at its highest default value: natural and chunk-generation hostile spawns have a `25%` chance to add one extra mob of the same type nearby. Extra spawns are capped at one per spawn event, obey normal spawn/collision checks, cannot recursively trigger more extras, and are skipped when the local hostile density cap is already reached.

## Mob-Specific Tier 5 Behavior

- Zombies, husks, drowned, zombie villagers, and zombified piglins can receive upgraded vanilla armor and melee weapons. Drowned holding unrecognized weapons such as tridents are left alone.
- Skeletons and strays can receive armor, bow enchantments, shield-counter movement, flanking, smarter ranged positioning, and improved projectile aim.
- Wither skeletons can receive armor and melee weapon upgrades.
- Pillagers can receive Quick Charge, possible Multishot, smarter ranged positioning, and improved projectile aim.
- Creepers pressure from farther away, flank, react to nearby attackers, and can briefly retreat before re-engaging.
- Spiders and cave spiders flank, lunge toward ranged targets, seek elevated angles, and can retreat/re-engage after damage.
- Witches keep spacing, use ally-aware movement, heal earlier at high tiers, and add cooldown-based slowness, weakness, and harming potion opportunities.
- Endermen use tactical side teleports and stronger spacing against close melee pressure.
- Endermites get fast side-approach melee pressure, pursuit persistence, and shared targeting at tier 4+ through the shared target goal.
- Evokers keep distance from melee players, reposition after spell casting, avoid standing still too long, and use nearby melee illagers as cover at tier 4+.
- Guardians reposition when line of sight is blocked, coordinate pressure with nearby guardians at tier 4+, and can prioritize exposed lower-health targets at tier 5. Elder guardian changes stay slower and conservative.
- Shulkers avoid continuing pressure through blocked line of sight, coordinate targets with nearby shulkers at tier 4+, and use slightly tighter cooldowns at tier 5.
- Slimes and magma cubes use better angled jump pressure, lightweight hazard checks, and stronger large-mob pressure at tier 5.
- Ravagers get target persistence, more deliberate pressure movement, close-range punishment positioning, and nearby raider coordination at tier 4+.
- Wardens only get conservative target persistence at tier 4+ and a conservative response to repeated ranged attackers at tier 5. Their default speed, damage, tracking, and sonic boom frequency are not increased.
- Ghasts, blazes, phantoms, and vexes use airborne positioning with target prediction at tier 5.
- Piglins, piglin brutes, hoglins, zoglins, vindicators, silverfish, and similar melee mobs use adaptive melee positioning when enabled.

## Shared Tactical AI

These systems are intentionally small tactical nudges, not hard counters:

- Group morale: mobs press more boldly when nearby allies outnumber nearby players, and isolated low-health mobs can briefly move defensively.
- Threat memory: mobs briefly remember the last player who damaged them so breaking line of sight does not instantly clear pressure.
- Sound investigation: tier 2+ mobs can move toward nearby recent block breaking, eating, bow shots, and explosions on cooldown.
- Target priority: mobs occasionally prefer low-health players, eating players, players drawing bows, lightly armored players, or players already attacking another mob.
- Equipment awareness: skeletons value unshielded players more, melee mobs value low-armor players more, and witches use weakness more readily against high-damage melee weapons.
- Light awareness: adaptive movement can prefer darker side-approach positions when both sides are otherwise viable.
- Retreat thresholds: witches, skeletons, pillagers, spiders, and vexes can briefly retreat at low health. Zombies mostly do not retreat.
- Anti-cheese movement: zombies gather under pillared players instead of wandering randomly, ranged mobs reposition against blocked/tiny-window shots, creepers hesitate across exposed visible ground, and endermen disengage from water traps.
- Support behavior: witches can support wounded nearby hostile mobs, melee mobs can peel toward players attacking ranged allies, and ranged mobs can hold back when melee allies are already pressuring.
- Escape denial: zombies and spiders imperfectly bias toward likely player exits and movement routes instead of always taking the shortest direct path.
- Biome and terrain tactics: Nether mobs are a little bolder in the Nether, drowned press better in water, spiders prefer vertical movement in caves, and skeletons space more in open terrain.
- Ranged cover: skeletons, pillagers, witches, and evokers can sometimes move toward imperfect cover after attacking.
- Pack roles: repeated mobs naturally spread by using side approaches based on entity id; no new entity types or variants are added.
- Fairness: dangerous behaviors keep vanilla warning windows and are implemented as movement/targeting goals with cooldowns.

## Commands

All commands require permission level 2 or higher.

- `/adaptivemobs get` shows current tier, world day, total hostile kills, and manual override status.
- `/adaptivemobs setglobaltier <0-5>` sets a manual global tier override.
- `/adaptivemobs reset` clears progression and manual tier override.
- `/adaptivemobs debug` prints current config/runtime summary.

## Configuration

Forge common config is generated at `config/adaptivemobs-common.toml`.

Important options include:

- `general.enableAdaptiveMobs`
- `general.maxTier`
- `mobs.zombies`
- `mobs.skeletons`
- `mobs.creepers`
- `mobs.spiders`
- `mobs.witches`
- `mobs.pillagers`
- `mobs.otherHostiles`
- `progression.dayThresholds`
- `progression.killThresholds`
- `progression.perTypeKillsPerTier`
- `scaling.enabled`
- `scaling.healthPerTier`
- `scaling.damagePerTier`
- `scaling.speedPerTier`
- `scaling.followRangePerTier`
- `scaling.armorPerTier`
- `scaling.knockbackResistPerTier`
- `gear.enabled`
- `gear.enchantChancePerTier`
- `spawnPressure.enabled`
- `spawnPressure.extraChancePerTier`
- `spawnPressure.densityRadius`
- `spawnPressure.densityCap`
- `ai.enabled`
- `ai.enableAdvancedAI`
- `ai.enableRangedAI`
- `ai.enableMeleeAI`
- `ai.enableEndermiteAI`
- `ai.enableEvokerAI`
- `ai.enableWitchAdvancedPotionAI`
- `ai.enableGuardianAdvancedAI`
- `ai.enableShulkerAdvancedAI`
- `ai.enableSlimeAdvancedAI`
- `ai.enableMagmaCubeAdvancedAI`
- `ai.enableRavagerAdvancedAI`
- `ai.enableWardenAdvancedAI`
- `ai.enableMoraleAI`
- `ai.enableTargetPriorityAI`
- `ai.enableAntiCheeseAI`
- `ai.enableDarkApproachAI`
- `ai.enableThreatMemoryAI`
- `ai.enableSoundInvestigationAI`
- `ai.enableSupportAI`
- `ai.enableEscapeDenialAI`
- `ai.enableBiomeTacticsAI`
- `ai.enableRangedCoverAI`
- `ai.skeletons`, `ai.zombies`, `ai.creepers`, `ai.spiders`, `ai.witches`, `ai.pillagers`
- `ai.endermen`, `ai.slimes`, `ai.ghasts`, `ai.blazes`, `ai.guardians`, `ai.phantoms`
- `ai.ravagers`, `ai.evokers`, `ai.vexes`, `ai.piglinFamily`, `ai.silverfishAndEndermites`, `ai.shulkers`, `ai.warden`

The defaults are intentionally moderate. Speed scaling is kept low to avoid unfair mobs.

## Compatibility Notes

The mod uses Forge events, saved data, attributes, vanilla equipment, and goal injection. It avoids mixins and client-only code. Spawn pressure is guarded against recursion, limited to one extra mob per natural spawn event, and capped by nearby hostile density.

Some advanced behavior is intentionally conservative. For example, skeleton shield response and mob flanking are implemented as small movement goals rather than full vanilla AI replacement.

## Not Yet Implemented

The following ideas require persisted state or broader balancing and are not currently active:

- Difficulty decay after repeated player deaths.
- Per-dimension or per-region progression, such as Nether mobs evolving primarily from Nether activity.
- Long-term player habit memory across sessions.

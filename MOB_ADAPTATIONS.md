# Mob Adaptations

This file describes how Adaptive Mobs changes each supported vanilla hostile mob or mob family. The mod does not add new mobs, models, variants, items, or assets. It adapts existing mobs through progression tiers, attribute modifiers, vanilla equipment changes, spawn pressure, and server-side goal injection.

## Shared Progression

Adaptive tiers run from 0 to 5. Tier 0 is vanilla behavior. By default, tiers increase from world day or total hostile kills:

- Tier 1: day 5 or 25 hostile kills.
- Tier 2: day 15 or 100 hostile kills.
- Tier 3: day 30 or 250 hostile kills.
- Tier 4: day 60 or 600 hostile kills.
- Tier 5: day 100 or 1200 hostile kills.

Each mob type can also gain an extra tier bonus from repeated kills of that same registry entity type. The default per-type threshold is 150 kills per bonus tier.

## Shared Scaling

When stat scaling is enabled, participating hostile mobs can receive:

- More max health.
- More attack damage when the mob has an attack damage attribute.
- Small movement speed bonuses at higher tiers.
- More follow range.
- Flat armor.
- Knockback resistance.
- A heal to the new maximum health when scaling is first applied.

Scaling is applied once per mob instance and stored in the mob's persistent data so it does not stack after chunk reloads.

## Shared Spawn Pressure

Natural and chunk-generation hostile spawns can occasionally add one extra mob of the same type nearby. The chance scales by tier and defaults to 40% at tier 5.

Extra spawns:

- Use the same vanilla entity type.
- Resolve candidate positions to a nearby ground surface before vanilla spawn checks.
- Respect collision, obstruction, light, biome, and vanilla spawn rules.
- Are capped by local hostile density.
- Cannot recursively create more extra spawns.

Tier 5 spawn pressure is intentionally more noticeable than earlier tiers. The local density cap still prevents runaway swarms, but high-tier areas should feel actively occupied instead of merely containing tougher individual mobs.

## Shared AI Systems

Several adaptive AI systems can apply across many mobs:

- Valid target enforcement: adaptive goals, remembered threats, shared targets, sound reactions, projectile adjustments, and special abilities ignore creative and spectator players. A priority target-sanitizer goal clears invalid targets promptly, and adaptive zombie stacks dismount when their target is no longer valid or the vertical obstacle is gone.
- Friendly-fire discipline: tier 2-5 hostile mobs ignore the first 1-4 accidental projectile hits respectively from the same hostile mob within a five-second window. Ignored hits deal no damage and do not trigger vanilla retaliation, so one Skeleton arrow no longer makes a higher-tier Zombie immediately attack the Skeleton. Sustained allied fire beyond the tier's tolerance still permits normal damage and retaliation.
- Shared targets: tier 4+ mobs can help nearby hostile allies focus a target. This does not create a permanent pack brain; it is a short-range goal that encourages nearby mobs to converge on a player already being pressured.
- Target priority: tier 3+ mobs can occasionally switch to a better target instead of blindly keeping the first target they saw. A player scores as more attractive if they are low health, eating, drawing or using a ranged weapon, lightly armored, already committed to attacking another mob, or exposed to skeleton fire without a shield.
- Threat memory: mobs can briefly remember the player who damaged them. This helps prevent trivial line-of-sight resets where a player hits a mob, steps behind a corner, and immediately drops all pressure.
- Sound investigation: tier 2+ mobs can move toward recent server-side sound events tracked by the mod: block breaking, eating, bow shots, and explosions. This is not full hearing AI; it is a short-lived point of interest that nearby mobs can investigate on cooldown.
- Morale: tier 3+ mobs compare nearby hostile allies to nearby non-creative, non-spectator players. If hostile allies outnumber players, they can move into darker or side-pressure positions more confidently. If a mob is isolated and below 35% health, it can briefly move defensively instead.
- Support: tier 3+ mobs can react to nearby allies. Melee mobs can move toward a player who is attacking a ranged ally such as a skeleton, pillager, or witch. Ranged mobs can step back when multiple melee allies are already attacking the same target, which helps avoid crowding.
- Escape denial: tier 3+ zombies and spiders can move toward where the player is looking, roughly three blocks ahead of the player, to pressure likely exits. If that position is not usable ground, they fall back to a nearby darker side position. This is intentionally imperfect and does not pathfind to every possible escape route.
- Anti-cheese behavior: tier 3+ selected mobs react to common static tactics that often trivialize vanilla mobs. "Cheese" here means repeatable low-risk setups such as pillaring above zombies, shooting skeletons through tiny blocked angles, baiting creepers across open visible ground, or standing in/near water to neutralize endermen. The mod does not indiscriminately alter terrain or hard-counter the player; block interaction is limited to recognized trap blocks and Enderman ramp scaffolds:
  - Hostile pathfinder mobs break nearby trap vehicles such as boats and minecarts, dismount if caught in one, and can move to free a nearby trapped hostile ally. The appropriate boat or minecart item is dropped when a vehicle is broken.
  - Hostile pathfinder mobs break nearby cobwebs, powder snow, and sweet berry bushes while escaping, with suitable drops for cobwebs and berry bushes. Spiders retain their natural immunity to cobweb trapping and do not break cobwebs.
  - Zombies under a pillared player gather near the player instead of wandering away, and can stack onto nearby zombies to climb toward pillared players or get over simple wall setups. The response now remains active for a short pressure window rather than performing only one movement check.
  - Creepers independently avoid boats, minecarts, cobwebs, powder snow, and sweet berry bushes along their approach path so their higher-priority pressure goal does not walk them into obvious traps.
  - Skeletons and pillagers with blocked line of sight reposition to a side or darker angle instead of staring at a wall.
  - Creepers in open ground with the player watching them stop swelling and try a side approach rather than walking straight forward. Creepers also cancel swelling against an actively blocking shield and try to move around the shield instead.
- Endermen near water, or fighting a player in water, retreat to safer ground.
- Biome tactics: tier 3+ mobs can make small terrain-specific decisions. Nether mobs are more aggressive in the Nether at close range, with shorter tactical cooldowns and stronger pressure at tiers 4-5. Drowned press harder when both they and the target are in water. Spiders in caves prefer elevated movement. Skeletons in open sky terrain try to keep wider side spacing.
- Ranged cover: tier 3+ ranged mobs can sometimes move sideways or backward after attacking. The "cover" is deliberately imperfect: the goal picks a reasonable retreat or side position rather than searching for a perfect protected bunker.

## Common Terms

Some repeated terms in the mob sections have specific meanings:

- Flanking: the mob moves to a side position around the target instead of always taking the shortest direct path.
- Side pressure: similar to flanking, but usually closer and less dramatic. It makes groups spread around the target instead of stacking in one line.
- Adaptive melee positioning: melee mobs choose a nearby side or pressure position and move there at a modest speed instead of always walking straight at the target. Conservative melee mobs use slower, safer pressure.
- Airborne repositioning: flying mobs choose a side position around the target, add a vertical offset, and use move control to drift there. At tier 5, they can include a small amount of target movement prediction.
- Target prediction: at higher tiers, some movement or projectile logic aims partly toward where the target is moving, not only where the target is now.
- Low-health retreat: a mob with low health can briefly back away or reposition, but it does not become permanently passive.
- Re-engage: after a short retreat or reposition, the mob continues attacking.
- Conservative behavior: the goal is intentionally limited so it does not override the vanilla mob's core identity or remove normal counterplay.

## Unique Late-Tier Abilities

Several hostile families also have identity-specific pressure tools. These use vanilla effects, movement, targeting, temporary blocks, or projectile metadata; the mod still does not add new items, entities, models, or permanent terrain changes.

- Zombies, husks, and zombie villagers - Corpse Ladder: the existing zombie stacking behavior remains the main anti-pillar tool. At tier 5, a stacked zombie can briefly surge upward when several zombies are under a player who is only slightly above them. This makes short pillars and low walls unreliable without letting zombies break blocks.
- Drowned - Dragging Grip: tier 4+ drowned that hit a player while both are in water apply brief Slowness and pull the player downward. This makes oceans, ruins, and flooded caves less automatically safe.
- Zombified piglins - Blood Call: at tier 5, damaging one zombified piglin can briefly grant nearby zombified piglins Speed and Strength against the attacker.
- Skeletons - Venom Shot: tier 4+ skeleton arrows have an uncommon Poison I roll for 4-6 seconds. At tier 5, skeletons can also add short Weakness against shielding targets or Slowness against sprinting targets.
- Strays - Frostbite Shot: tier 4+ stray arrows apply a stronger Slowness pulse, with occasional Mining Fatigue at tier 5.
- Wither skeletons - Execution Swing: tier 4+ wither skeletons refresh Wither and add extra shove pressure when hitting a target below 40% health.
- Creepers - Blink Shroud: tier 5 creepers near detonation range can briefly turn invisible for about half a second before pressure resumes. A primed creeper sound plays as the cue.
- Spiders - Web Retreat: tier 4+ spiders can place a temporary cobweb at or near the player after taking damage or landing a hit. The cobweb decays after roughly 5-8 seconds. Web placement now has a substantially longer per-spider cooldown, preventing repeated web spam.
- Cave spiders - Venom Pounce: cave spiders keep their lunge behavior against ranged/eating players and refresh short Poison on hits against players using bows, crossbows, or items.
- Witches - Brewed Combo: witches already support allies with regeneration and punish rushes with Slowness, Weakness, or Harming. At tier 5 these potion opportunities happen earlier and make witches priority targets.
- Pillagers - Pinning Bolt: tier 5 pillager arrows can carry a short pinned effect: Slowness and a one-second shield disable on hit.
- Endermen - Backstep Ambush: tier 4+ endermen hit in melee can teleport behind or diagonally behind the attacker and immediately re-engage, with a shorter cooldown at tier 5.
- Endermites - Swarm Mark: endermite hits mark the target for nearby endermites and endermen, causing them to prioritize the same player.
- Silverfish - Burrow Burst: tier 4+ low-health silverfish can briefly vanish, then reappear beside their attacker after a short delay.
- Evokers - Vex Relay: tier 4+ evokers that are rushed can redirect nearby vexes onto the attacker while the evoker backs away.
- Vindicators - Armor Breaker: tier 5 vindicator axe hits add a small armor-piercing damage component and briefly disable an active shield.
- Ravagers - Crushing Charge: tier 4+ ravager hits can deliver heavy sideways/upward knockback on cooldown, with bonus damage if the player is slammed into a wall.
- Guardians - Focused Beam: tier 4+ guardian beam damage increases slightly when multiple guardians are already focused on the same player.
- Elder guardians - Drowning Pressure: elder guardian beam hits add brief Slowness and a sinking pull.
- Shulkers - Gravity Chain: tier 4+ shulker hits against an already levitating player extend Levitation and apply brief Slowness.
- Slimes - Recombine: tier 5 equal-size Slimes that survive for 10-15 seconds seek one another within 12 blocks. On contact, one absorbs the other and doubles in size from 1 to 2 or from 2 to 4.
- Magma cubes - Molten Recombine: tier 5 Magma Cubes use the same delayed merge behavior, but only combine with equal-size Magma Cubes and never mix with ordinary Slimes.
- Piglins - Gold Frenzy: piglin-family Nether pressure is stronger at tier 5, while gold still remains valid counterplay rather than a hard immunity bypass.
- Piglin brutes - Guard Break: tier 5 brute hits add a small armor-piercing damage component and briefly disable shields.
- Hoglins - Gore Toss: tier 4+ hoglin hits can toss the player upward and backward on cooldown.
- Zoglins - Frenzied Rampage: tier 5 low-health zoglins gain a short Speed and Resistance burst.
- Ghasts - Splitting Fireball: every tier 5 Ghast fireball launches two additional fireballs at shallow side angles while preserving the original center shot. Split projectiles are marked so they cannot recursively split again.
- Blazes - Overheat Volley: tier 5 blazes that take ranged damage briefly gain Speed and Strength pressure to punish static bow farming.
- Phantoms - Bleeding Dive: tier 5 phantom hits apply brief Hunger as a vanilla bleeding-style damage-over-time pressure.
- Vexes - Phase Cut: tier 5 vexes that hit while phasing gain bonus hit pressure and apply brief Weakness.
- Wardens - Echo Mark: tier 5 wardens remember repeated ranged attackers longer. The next sonic boom against a marked player adds extra knockback and briefly suppresses sprint escape with Slowness, without increasing sonic boom frequency.

## Zombies, Husks, Drowned, Zombie Villagers, and Zombified Piglins

Participation toggle: `mobs.zombies`  
AI toggles: `ai.zombies`, `ai.enableMeleeAI`, `ai.enableEscapeDenialAI`, `ai.enableAntiCheeseAI`

These mobs receive the shared scaling and spawn pressure systems. They can also receive armor and melee weapon upgrades. At tier 5, armor coverage and diamond/iron rolls are much more common, and melee weapon enchantments can become meaningfully dangerous. The gear system respects existing gear and does not replace unrecognized held items, so drowned with tridents are left alone.

AI changes:

- Basic flanking from tier 1+ when zombie AI is enabled.
- Adaptive melee positioning, with small side pressure and target prediction at high tiers.
- Escape-denial movement toward the direction the player appears to be moving or looking, so zombies sometimes pressure an exit instead of trailing directly behind.
- Anti-cheese movement against pillared players: if the player is more than 2.5 blocks above the zombie and still nearby horizontally, the zombie moves toward a close side position under or around the pillar.
- Zombies can stack by riding nearby zombies when a player is above them on a pillar or behind a simple vertical obstacle. This gives groups a way to build pressure upward without breaking blocks.
- Stacked zombies coordinate through the bottom carrier, push upward and toward the target as a corpse ladder, and dismount near attack height. They automatically unstack when the target moves away, descends, becomes invalid, or the obstacle no longer requires a stack.
- The same vertical-pressure response recognizes nearby simple walls as well as player-built pillars. Zombies gather and hop against the obstacle when no suitable carrier is available.
- Tier 4+ shared targeting with nearby hostile mobs.

Zombified piglins also count as zombies for gear and participation, but Nether-family melee AI settings affect some piglin-family movement behavior elsewhere.

## Skeletons, Strays, and Wither Skeletons

Participation toggle: `mobs.skeletons`  
AI toggles: `ai.skeletons`, `ai.enableRangedAI`, `ai.enableRangedCoverAI`, `ai.enableAntiCheeseAI`, `ai.enableBiomeTacticsAI`

These mobs receive shared scaling and spawn pressure. Skeletons and strays can receive armor and bow enchantments. Late-tier bows are allowed to matter through stronger Power rolls and occasional Punch or Flame at tier 5. Wither skeletons can receive armor and melee weapon upgrades if they do not use a bow.

AI changes:

- Shield-aware behavior that softens or adjusts pressure against blocking targets.
- Flanking and spacing goals.
- Improved ranged attack positioning and small projectile aim correction.
- Possible upward aim bias at high tiers.
- Low-health retreat behavior.
- Ranged-cover repositioning after attacks: the skeleton may step backward or to a side angle if the target is within 16 blocks.
- Open-terrain spacing through biome tactics.
- Anti-cheese repositioning against blocked or tiny-window shots: if a skeleton or pillager cannot see its target, it periodically tries a side or darker angle instead of standing still.
- Tier 4+ shared targeting.

## Creepers

Participation toggle: `mobs.creepers`  
AI toggles: `ai.creepers`, `ai.enableCreeperAI`, `ai.enableAntiCheeseAI`

Creepers receive shared scaling and spawn pressure.

AI changes:

- Fuse duration decreases with tier: 30 ticks at tier 1, then 27, 24, 21, and 18 ticks at tiers 2-5 respectively.
- Tier 2+ creepers cancel premature swelling if they are still too far away.
- Tier 3+ creepers use pressure logic around when to start swelling.
- Tier 4+ creepers can commit when another nearby attacker is already pressuring the target.
- Tier 5 creepers can briefly retreat and re-engage instead of always walking directly forward.
- Anti-cheese behavior can make open-ground approaches less predictable: if a creeper is visible to the target, under open sky, and still more than 4 blocks away, it cancels swelling and tries to reposition sideways.
- Creepers do not intentionally explode into an actively blocking shield. They cancel swelling at close range and path to a side angle before trying to pressure again.
- Creepers periodically verify that the target is actually reachable. If no complete path exists, they cancel swelling and keep deliberate spacing instead of charging or priming uselessly.
- Creepers detect nearby trap vehicles and trap blocks, including hazards directly ahead of their approach, and move away before resuming pressure.
- Tier 4+ shared targeting applies.

## Spiders and Cave Spiders

Participation toggle: `mobs.spiders`  
AI toggles: `ai.spiders`, `ai.enableSpiderAI`, `ai.enableMeleeAI`, `ai.enableEscapeDenialAI`, `ai.enableBiomeTacticsAI`

Spiders receive shared scaling and spawn pressure. Cave spiders use the same family logic with faster and earlier tactical movement.

AI changes:

- Flanking with close-range side pressure.
- Tier 2+ cave spiders and tier 3+ spiders use tactical side movement.
- Tier 3+ spiders can lunge at nearby players using ranged weapons.
- Tier 3+ spiders actively climb toward pillared targets for a sustained window. They move to the pillar face and add upward movement while against it; cave spiders climb slightly faster and maintain the attempt longer.
- Tier 4+ spiders prefer elevated positions when practical.
- Cave or underground spiders can use vertical terrain pressure.
- Retreat and re-engage behavior after damage. When spiders disengage, they gain invisibility and regeneration 5 for 5 seconds while repositioning before re-engaging.
- Low-health retreat behavior.
- Escape-denial movement toward likely player exits, based on the direction the player is looking.
- Tier 4+ shared targeting.

## Witches

Participation toggle: `mobs.witches`  
AI toggles: `ai.witches`, `ai.enableWitchAI`, `ai.enableWitchAdvancedPotionAI`, `ai.enableRangedAI`, `ai.enableRangedCoverAI`

Witches receive shared scaling and spawn pressure.

AI changes:

- Flanking and spacing when players get close.
- Extra potion opportunities against moving, close, or weakened targets.
- Earlier self-healing thresholds at higher tiers.
- Regeneration support potions for wounded nearby hostile allies.
- Slowness against sprinting or fast-moving targets.
- Weakness against melee threats.
- Harming pressure against low-health targets at tier 5.
- Low-health retreat behavior.
- Ranged-cover repositioning: the witch may step backward or to a side angle if the target is within 16 blocks.
- Tier 4+ shared targeting.

## Pillagers

Participation toggle: `mobs.pillagers`  
AI toggles: `ai.pillagers`, `ai.enablePillagerAI`, `ai.enableRangedAI`, `ai.enableRangedCoverAI`, `ai.enableRaidMobAI`, `ai.enableAntiCheeseAI`

Pillagers receive shared scaling and spawn pressure. Their crossbows can gain Quick Charge at tier 3+ and occasional Multishot or Piercing at tier 5.

AI changes:

- Flanking and ranged repositioning.
- Projectile aim correction at higher tiers.
- Ranged-cover movement after attacks: the pillager may step backward or to a side angle if the target is within 16 blocks.
- Low-health retreat behavior.
- Anti-cheese ranged behavior when line of sight is blocked, using the same side-angle repositioning as skeletons.
- Tier 4+ shared targeting.

## Endermen

Participation toggle: `mobs.otherHostiles`  
AI toggles: `ai.endermen`, `ai.enableEndermanAI`, `ai.enableAntiCheeseAI`

Endermen receive shared scaling and spawn pressure through the generic hostile toggle.

AI changes:

- Tier 2+ tactical teleport checks.
- Tier 3+ angular side teleports around a target.
- Water-aware teleporting away from unsafe positions.
- Tier 5 wider side repositioning against close melee pressure.
- In the End, tier 4+ endermen can coordinate nearby endermen onto the same target, with wider coordination and more teleport attempts at tier 5.
- Tier 4+ endermen answer nearby towered players by constructing a supported diagonal ramp from vanilla solid blocks. They place steps every 4-7 ticks, keep a stable ramp plan while climbing, and teleport onto each new step so they do not wait exposed beneath the tower.
- Endermen preserve a useful carried solid block for the ramp. An unsuitable carried block is safely placed nearby when possible, or dropped before a scaffold block is selected.
- Anti-cheese response for water traps: if the enderman or target is in water, the enderman tries to retreat to a safer position instead of staying in a bad spot.
- Tier 4+ shared targeting.

## Endermites

Participation toggle: `mobs.otherHostiles`  
AI toggles: `ai.silverfishAndEndermites`, `ai.enableEndermiteAI`, `ai.enableMeleeAI`

Endermites receive shared scaling and spawn pressure through the generic hostile toggle.

AI changes:

- Tier 2+ side pursuit.
- Remembered target persistence over short distances.
- Faster movement at tier 5.
- Target prediction at tier 5.
- Tier 4+ shared targeting.

## Silverfish

Participation toggle: `mobs.otherHostiles`  
AI toggles: `ai.silverfishAndEndermites`, `ai.enableMeleeAI`

Silverfish receive shared scaling and spawn pressure through the generic hostile toggle.

AI changes:

- Adaptive melee positioning.
- Slightly faster melee pressure than larger generic melee mobs.
- Tier 4+ shared targeting.

## Evokers

Participation toggle: `mobs.otherHostiles`  
AI toggles: `ai.evokers`, `ai.enableEvokerAI`, `ai.enableRaidMobAI`, `ai.enableRangedCoverAI`

Evokers receive shared scaling and spawn pressure through the generic hostile toggle.

AI changes:

- Tier 2+ repositioning after spell casts, after standing still, or when rushed by melee players.
- Tier 4+ use nearby illagers as pressure support while repositioning.
- Ranged-cover movement: the evoker may step backward or to a side angle if the target is within 16 blocks.
- Tier 4+ shared targeting.

## Vindicators

Participation toggle: `mobs.otherHostiles`  
AI toggles: `ai.enableMeleeAI`, `ai.enableRaidMobAI`, `ai.ravagers`

Vindicators receive shared scaling and spawn pressure through the generic hostile toggle.

AI changes:

- Adaptive melee positioning when raid melee AI is enabled.
- Conservative melee pressure as part of the raid-mob path.
- Tier 4+ shared targeting.

## Ravagers

Participation toggle: `mobs.otherHostiles`  
AI toggles: `ai.ravagers`, `ai.enableRaidMobAI`, `ai.enableRavagerAdvancedAI`, `ai.enableMeleeAI`

Ravagers receive shared scaling and spawn pressure through the generic hostile toggle.

AI changes:

- Tier 3+ target persistence.
- Deliberate pressure movement instead of only direct pursuit.
- Short retreat positioning when too close.
- Tier 4+ coordination with nearby illagers.
- Tier 5 stronger pressure when the target is visible and within range.
- Conservative adaptive melee positioning.
- Tier 4+ shared targeting.

## Guardians and Elder Guardians

Participation toggle: `mobs.otherHostiles`  
AI toggles: `ai.guardians`, `ai.enableAquaticHostileAI`, `ai.enableGuardianAdvancedAI`, `ai.enableRangedAI`

Guardians and elder guardians receive shared scaling and spawn pressure through the generic hostile toggle. Elder guardian tactics are intentionally slower and more conservative.

AI changes:

- Tier 2+ repositioning when line of sight is blocked.
- Tier 4+ non-elder guardians can coordinate pressure with nearby attackers.
- Tier 5 non-elder guardians can retarget toward more vulnerable visible targets.
- Ranged positioning support.
- Tier 4+ shared targeting.

## Shulkers

Participation toggle: `mobs.otherHostiles`  
AI toggles: `ai.shulkers`, `ai.enableEndMobAI`, `ai.enableShulkerAdvancedAI`, `ai.enableRangedAI`

Shulkers receive shared scaling and spawn pressure through the generic hostile toggle.

AI changes:

- Tier 2+ line-of-sight checks that stop pressure if the target stays blocked.
- Tier 4+ nearby shulkers can coordinate onto the same target.
- Tier 5 uses shorter tactical cooldown windows, especially in the End, and coordinates over a wider local radius.
- Ranged positioning support.
- Tier 4+ shared targeting.

## Slimes

Participation toggle: `mobs.otherHostiles`  
AI toggles: `ai.slimes`, `ai.enableSlimeAdvancedAI`, `ai.enableMeleeAI`

Slimes receive shared scaling and spawn pressure through the generic hostile toggle.

AI changes:

- Tier 2+ angled jump pressure instead of only direct movement.
- Side positioning based on entity id so repeated slimes naturally spread.
- Larger tier 5 slimes move and leap more aggressively.
- Tier 5 equal-size Slimes become eligible to recombine after surviving for 10-15 seconds, seek a compatible partner within 12 blocks, and merge on contact. Two size-1 Slimes become size 2; two size-2 Slimes become size 4. Size-4 Slimes do not grow further.
- Adaptive melee positioning.
- Tier 4+ shared targeting.

## Magma Cubes

Participation toggle: `mobs.otherHostiles`  
AI toggles: `ai.slimes`, `ai.enableMagmaCubeAdvancedAI`, `ai.enableMeleeAI`, `ai.enableBiomeTacticsAI`

Magma cubes receive shared scaling and spawn pressure through the generic hostile toggle.

AI changes:

- The same angled jump pressure as slimes, controlled by magma-cube advanced AI.
- Tier 5 Magma Cubes use the same 10-15 second recombination cycle and 1→2→4 size progression, but only with other Magma Cubes of exactly the same size.
- Adaptive melee positioning.
- Nether biome tactics make them bolder in the Nether.
- Tier 4+ shared targeting.

## Piglins, Piglin Brutes, Hoglins, Zoglins, and Zombified Piglins

Participation toggle: `mobs.otherHostiles` for piglins, brutes, hoglins, and zoglins; `mobs.zombies` for zombified piglins  
AI toggles: `ai.piglinFamily`, `ai.enableNetherMobAI`, `ai.enableMeleeAI`, `ai.enableBiomeTacticsAI`

These mobs receive shared scaling and spawn pressure when their participation toggle allows it. Piglin gear is intentionally left alone to avoid disturbing bartering behavior.

AI changes:

- Adaptive melee positioning.
- Conservative pressure for piglin brutes.
- Nether biome tactics make the family bolder in the Nether.
- Tier 4+ shared targeting.

## Ghasts

Participation toggle: `mobs.otherHostiles`  
AI toggles: `ai.ghasts`, `ai.enableNetherMobAI`, `ai.enableRangedAI`

Ghasts receive shared scaling and spawn pressure through the generic hostile toggle.

AI changes:

- Ranged positioning support with a longer preferred range.
- Airborne repositioning around targets.
- Larger vertical offset than other airborne mobs.
- Tier 5 target prediction for airborne positioning.
- Tier 5 Splitting Fireball adds two shallow-angle side projectiles to each normal center fireball. The side shots retain the Ghast as owner and use its normal explosion power.
- Nether biome tactics identify them as Nether mobs where applicable.
- Tier 4+ shared targeting.

## Blazes

Participation toggle: `mobs.otherHostiles`  
AI toggles: `ai.blazes`, `ai.enableNetherMobAI`, `ai.enableRangedAI`

Blazes receive shared scaling and spawn pressure through the generic hostile toggle.

AI changes:

- Ranged positioning support.
- Airborne side repositioning around targets.
- Tier 5 target prediction for airborne positioning.
- Nether biome tactics make them bolder in the Nether.
- Tier 4+ shared targeting.

## Phantoms

Participation toggle: `mobs.otherHostiles`  
AI toggle: `ai.phantoms`

Phantoms receive shared scaling and spawn pressure through the generic hostile toggle.

AI changes:

- Airborne repositioning around targets.
- Higher vertical offset than most non-ghast airborne mobs.
- Tier 5 target prediction for airborne positioning.
- Tier 4+ shared targeting.

## Vexes

Participation toggle: `mobs.otherHostiles`  
AI toggles: `ai.vexes`, `ai.enableRaidMobAI`

Vexes receive shared scaling and spawn pressure through the generic hostile toggle.

AI changes:

- Airborne repositioning around targets.
- Retreat and re-engage behavior.
- Low-health retreat behavior.
- Tier 5 target prediction for airborne positioning.
- Tier 4+ shared targeting.

## Wardens

Participation toggle: `mobs.otherHostiles`  
AI toggles: `ai.warden`, `ai.enableWardenAI`, `ai.enableWardenAdvancedAI`, `ai.enableMeleeAI`

Wardens receive shared scaling and spawn pressure only if generic hostiles are enabled. Their adaptive AI is intentionally conservative.

AI changes:

- Conservative adaptive melee positioning.
- Tier 4+ target persistence if a recent target remains nearby.
- Tier 5 response to repeated ranged attackers.

The mod does not intentionally increase the Warden's default speed, damage, tracking, or sonic boom frequency through custom Warden AI.

## Bosses

Boss AI toggle: `ai.enableBossAI`

The Ender Dragon and Wither are excluded by default. Boss support is treated as future-facing and should remain off unless explicitly enabled and tested.

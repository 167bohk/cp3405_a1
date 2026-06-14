package au.edu.jcu.cp3406_cp5307_utilityappstartertemplate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.edu.jcu.cp3406_cp5307_utilityappstartertemplate.ui.theme.CP3406_CP5603UtilityAppStarterTemplateTheme
import kotlin.math.roundToInt
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CP3406_CP5603UtilityAppStarterTemplateTheme {
                UtilityApp()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UtilityAppPreview() {
    CP3406_CP5603UtilityAppStarterTemplateTheme {
        UtilityApp()
    }
}

private enum class AppTab(val label: String) {
    Utility("Utility"),
    Settings("Settings")
}

private data class CardTemplate(
    val name: String,
    val description: String,
    val isPremium: Boolean,
    val count: Int
)

private data class GameCard(
    val name: String,
    val description: String,
    val isPremium: Boolean,
    val imageResId: Int
)

private class PlayerState(
    val name: String,
    val accent: Color,
    maxHp: Int = 4,
    currentHp: Int = 4,
    attackRange: Int = 3,
    maxEnergy: Int = 4,
    currentEnergy: Int = 4
) {
    var maxHp by mutableIntStateOf(maxHp)
    var currentHp by mutableIntStateOf(currentHp)
    var attackRange by mutableIntStateOf(attackRange)
    var maxEnergy by mutableIntStateOf(maxEnergy)
    var currentEnergy by mutableIntStateOf(currentEnergy)
    var evolutionPoints by mutableIntStateOf(0)
    var lastDiscardWasPremium by mutableStateOf(false)
    val hand = mutableStateListOf<GameCard>()
    val equipment = mutableStateListOf<GameCard>()
    val shownEquipment = mutableStateListOf<GameCard>()
}

private class GameSession {
    var selectedPlayerIndex by mutableIntStateOf(0)
    var eventMode by mutableStateOf(false)
    var showPremiumCards by mutableStateOf(true)
    var startingEnergy by mutableIntStateOf(4)
    var prompt by mutableStateOf("Select a player, draw cards, and track the current turn.")
    var promptWarning by mutableStateOf(false)

    val players = mutableStateListOf(
        PlayerState("Player 1", Color(0xFFC62828)),
        PlayerState("Player 2", Color(0xFF2E7D32)),
        PlayerState("Player 3", Color(0xFF1565C0)),
        PlayerState("Player 4", Color(0xFFF9A825)),
        PlayerState("Draft Pool", Color(0xFF6A1B9A))
    )
    val normalDeck = mutableStateListOf<GameCard>()
    val premiumDeck = mutableStateListOf<GameCard>()
    val normalDiscard = mutableStateListOf<GameCard>()
    val premiumDiscard = mutableStateListOf<GameCard>()

    init {
        resetDecks()
        dealStartingHands()
    }

    val selectedPlayer: PlayerState
        get() = players[selectedPlayerIndex]

    val totalNormalCards: Int
        get() = cardTemplates.filter { !it.isPremium }.sumOf { it.count }

    val totalPremiumCards: Int
        get() = cardTemplates.filter { it.isPremium }.sumOf { it.count }

    fun selectPlayer(index: Int) {
        selectedPlayerIndex = index
        showMessage("${players[index].name} is active.")
    }

    fun resetGame() {
        players.forEach { player ->
            player.maxHp = 4
            player.currentHp = 4
            player.attackRange = 3
            player.maxEnergy = startingEnergy
            player.currentEnergy = startingEnergy
            player.evolutionPoints = 0
            player.lastDiscardWasPremium = false
            player.hand.clear()
            player.equipment.clear()
            player.shownEquipment.clear()
        }
        selectedPlayerIndex = 0
        resetDecks()
        dealStartingHands()
        showMessage("New session started with ${players.size} players.")
    }

    fun setPlayerCount(count: Int) {
        val boundedCount = count.coerceIn(2, 5)
        while (players.size < boundedCount) {
            val next = players.size + 1
            players.add(PlayerState("Player $next", playerColors[(next - 1) % playerColors.size]))
        }
        while (players.size > boundedCount) {
            players.removeAt(players.lastIndex)
        }
        selectedPlayerIndex = selectedPlayerIndex.coerceAtMost(players.lastIndex)
        showMessage("Player count set to $boundedCount.")
    }

    fun updateStartingEnergy(value: Int) {
        startingEnergy = value.coerceIn(2, 8)
        players.forEach {
            it.maxEnergy = startingEnergy
            it.currentEnergy = it.currentEnergy.coerceAtMost(startingEnergy)
        }
    }

    fun drawCard(premium: Boolean) {
        if (premium && !showPremiumCards) {
            showMessage("Premium card drawing is disabled in Settings.", warning = true)
            return
        }

        val deck = if (premium) premiumDeck else normalDeck
        val discard = if (premium) premiumDiscard else normalDiscard
        if (deck.isEmpty() && discard.isNotEmpty()) {
            deck.addAll(discard)
            discard.clear()
            showMessage("${if (premium) "Premium" else "Normal"} deck was refilled from discard.", warning = true)
        }

        if (deck.isEmpty()) {
            showMessage("No ${if (premium) "premium" else "normal"} cards left to draw.", warning = true)
            return
        }

        val card = deck.removeAt(Random.nextInt(deck.size))
        selectedPlayer.hand.add(card)
        if (card.name == "Artifact") {
            selectedPlayer.currentEnergy = 0
        }
        showMessage("${selectedPlayer.name} drew ${card.name}: ${card.description}")
    }

    fun discardCard(card: GameCard, fromEquipment: Boolean) {
        val player = selectedPlayer
        val removed = if (fromEquipment) player.equipment.remove(card) else player.hand.remove(card)
        if (!removed) return

        if (card.name == "Artifact") {
            player.hand.add(card)
            showMessage("Artifact cannot be discarded.", warning = true)
            return
        }
        player.shownEquipment.remove(card)

        if (card.isPremium) {
            premiumDiscard.add(card)
        } else {
            normalDiscard.add(card)
        }
        player.lastDiscardWasPremium = card.isPremium
        showMessage("${player.name} discarded ${card.name}.")
    }

    fun equipCard(card: GameCard) {
        val player = selectedPlayer
        if (!player.hand.remove(card)) return
        player.equipment.add(card)
        showMessage("${player.name} equipped ${card.name}.")
    }

    fun showEquipment(card: GameCard) {
        val player = selectedPlayer
        if (card !in player.equipment) return
        if (card !in player.shownEquipment) {
            player.shownEquipment.add(card)
        }
        showMessage("${player.name} revealed ${card.name}.")
    }

    fun stealRandomHandCard(victim: PlayerState) {
        val thief = selectedPlayer
        if (victim == thief) return
        if (victim.hand.isEmpty()) {
            showMessage("${victim.name} has no hand cards to steal.", warning = true)
            return
        }
        val card = victim.hand.removeAt(Random.nextInt(victim.hand.size))
        thief.hand.add(card)
        showMessage("${thief.name} stole 1 random hand card from ${victim.name}.")
    }

    fun stealShownEquipment(victim: PlayerState, card: GameCard) {
        val thief = selectedPlayer
        if (victim == thief) return
        if (!victim.equipment.remove(card)) {
            showMessage("${card.name} is no longer available.", warning = true)
            return
        }
        victim.shownEquipment.remove(card)
        thief.hand.add(card)
        showMessage("${thief.name} stole ${card.name} from ${victim.name}.")
    }

    fun withdrawDiscard() {
        val player = selectedPlayer
        val source = if (player.lastDiscardWasPremium) premiumDiscard else normalDiscard
        if (source.isEmpty()) {
            showMessage("There is no discarded card to withdraw.", warning = true)
            return
        }
        val card = source.removeAt(source.lastIndex)
        player.hand.add(card)
        showMessage("${player.name} withdrew ${card.name}.")
    }

    fun modifyStat(stat: String, delta: Int) {
        val player = selectedPlayer
        when (stat) {
            "HP" -> player.currentHp = (player.currentHp + delta).coerceIn(0, player.maxHp)
            "Max HP" -> {
                player.maxHp = (player.maxHp + delta).coerceIn(1, 12)
                player.currentHp = player.currentHp.coerceAtMost(player.maxHp)
            }
            "Energy" -> player.currentEnergy = (player.currentEnergy + delta).coerceIn(0, player.maxEnergy)
            "Max Energy" -> {
                player.maxEnergy = (player.maxEnergy + delta).coerceIn(1, 12)
                player.currentEnergy = player.currentEnergy.coerceAtMost(player.maxEnergy)
            }
            "AR" -> player.attackRange = (player.attackRange + delta).coerceIn(1, 12)
            "EXP" -> player.evolutionPoints = (player.evolutionPoints + delta).coerceIn(0, 24)
        }
        showMessage("${player.name}'s $stat updated.")
    }

    fun resetEnergy() {
        selectedPlayer.currentEnergy = selectedPlayer.maxEnergy
        showMessage("${selectedPlayer.name}'s energy was reset.")
    }

    private fun resetDecks() {
        normalDeck.clear()
        premiumDeck.clear()
        normalDiscard.clear()
        premiumDiscard.clear()
        cardTemplates.forEach { template ->
            repeat(template.count) { index ->
                val card = GameCard(
                    name = if (template.count > 1) "${template.name} #${index + 1}" else template.name,
                    description = template.description,
                    isPremium = template.isPremium,
                    imageResId = cardImageResId(template.name)
                )
                if (template.isPremium) {
                    premiumDeck.add(card)
                } else {
                    normalDeck.add(card)
                }
            }
        }
    }

    private fun dealStartingHands() {
        repeat(4) {
            players.take(4).forEach { player ->
                if (normalDeck.isNotEmpty()) {
                    player.hand.add(normalDeck.removeAt(Random.nextInt(normalDeck.size)))
                }
            }
        }
    }

    private fun showMessage(message: String, warning: Boolean = false) {
        prompt = message
        promptWarning = warning
    }
}

@Composable
fun UtilityApp() {
    var selectedTab by remember { mutableStateOf(AppTab.Utility) }
    val session = remember { GameSession() }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = AppTab.Utility.label) },
                    label = { Text(AppTab.Utility.label) },
                    selected = selectedTab == AppTab.Utility,
                    onClick = { selectedTab = AppTab.Utility }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = AppTab.Settings.label) },
                    label = { Text(AppTab.Settings.label) },
                    selected = selectedTab == AppTab.Settings,
                    onClick = { selectedTab = AppTab.Settings }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                AppTab.Utility -> UtilityScreen(session)
                AppTab.Settings -> SettingsScreen(session)
            }
        }
    }
}

@Composable
private fun UtilityScreen(session: GameSession) {
    val player = session.selectedPlayer

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HeaderSection(session)
        }
        item {
            PlayerSelector(session)
        }
        item {
            PlayerSummary(player)
        }
        item {
            ActionPanel(session)
        }
        item {
            CardListSection(
                title = "Hand",
                emptyText = "No cards in hand.",
                cards = player.hand,
                primaryAction = "Equip",
                secondaryAction = "Discard",
                onPrimary = session::equipCard,
                onSecondary = { session.discardCard(it, fromEquipment = false) }
            )
        }
        item {
            CardListSection(
                title = "Equipment",
                emptyText = "No equipped cards.",
                cards = player.equipment,
                primaryAction = "Discard",
                secondaryAction = null,
                onPrimary = { session.discardCard(it, fromEquipment = true) },
                onSecondary = {}
            )
        }
    }
}

@Composable
private fun HeaderSection(session: GameSession) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Black Hole Artifact Companion",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Normal ${session.normalDeck.size}/${session.totalNormalCards}   Premium ${session.premiumDeck.size}/${session.totalPremiumCards}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = session.prompt,
            color = if (session.promptWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun PlayerSelector(session: GameSession) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        session.players.forEachIndexed { index, player ->
            AssistChip(
                onClick = { session.selectPlayer(index) },
                label = { Text(player.name) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(player.accent)
                    )
                }
            )
        }
    }
}

@Composable
private fun PlayerSummary(player: PlayerState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(player.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            TokenStatRow(
                label = "HP ${player.currentHp}/${player.maxHp}",
                activeCount = player.currentHp,
                inactiveCount = player.maxHp - player.currentHp,
                activeResId = R.drawable.token_hp_blue,
                inactiveResId = R.drawable.token_hp_red,
                tokenWidth = 28,
                tokenHeight = 28,
                spacing = (-1).dp
            )
            TokenStatRow(
                label = "EN ${player.currentEnergy}/${player.maxEnergy}",
                activeCount = player.currentEnergy,
                inactiveCount = player.maxEnergy - player.currentEnergy,
                activeResId = R.drawable.token_energy_blue,
                inactiveResId = R.drawable.token_energy_red,
                tokenWidth = 34,
                tokenHeight = 38,
                spacing = (-8).dp
            )
            EvolutionStatRow(player.evolutionPoints)
            StatText("AR", player.attackRange.toString())
            Text(
                text = "Hand ${player.hand.size}   Equipment ${player.equipment.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TokenStatRow(
    label: String,
    activeCount: Int,
    inactiveCount: Int,
    activeResId: Int,
    inactiveResId: Int,
    tokenWidth: Int,
    tokenHeight: Int,
    spacing: androidx.compose.ui.unit.Dp
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(activeCount.coerceAtLeast(0)) {
                TokenImage(activeResId, label, tokenWidth, tokenHeight)
            }
            repeat(inactiveCount.coerceAtLeast(0)) {
                TokenImage(inactiveResId, label, tokenWidth, tokenHeight)
            }
        }
    }
}

@Composable
private fun EvolutionStatRow(points: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "EXP $points/4",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (points == 0) {
            Text("No evolution points yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(points.coerceAtLeast(0)) { index ->
                    val resId = if (index % 2 == 0) R.drawable.token_exp_l else R.drawable.token_exp_r
                    TokenImage(resId, "Evolution point", 20, 34)
                }
            }
        }
    }
}

@Composable
private fun TokenImage(resId: Int, description: String, widthDp: Int, heightDp: Int) {
    Image(
        painter = painterResource(resId),
        contentDescription = description,
        modifier = Modifier
            .width(widthDp.dp)
            .height(heightDp.dp),
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun StatText(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ActionPanel(session: GameSession) {
    var showStatControls by remember { mutableStateOf(false) }
    var showEquipmentControls by remember { mutableStateOf(false) }
    var showStealControls by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = { session.drawCard(premium = false) }
            ) {
                Text("Draw Normal")
            }
            Button(
                modifier = Modifier.weight(1f),
                onClick = { session.drawCard(premium = true) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text("Draw Premium")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = session::withdrawDiscard
            ) {
                Text("Withdraw")
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = session::resetEnergy
            ) {
                Text("Reset EN")
            }
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showStatControls = !showStatControls }
        ) {
            Text(if (showStatControls) "Hide Stat Controls" else "Show Stat Controls")
        }
        if (showStatControls) {
            StatControls(session)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { showEquipmentControls = !showEquipmentControls }
            ) {
                Text(if (showEquipmentControls) "Hide Equipment" else "Show Equipment")
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { showStealControls = !showStealControls }
            ) {
                Text(if (showStealControls) "Hide Steal" else "Steal")
            }
        }
        if (showEquipmentControls) {
            ShowEquipmentPanel(session)
        }
        if (showStealControls) {
            StealPanel(session)
        }
    }
}

@Composable
private fun ShowEquipmentPanel(session: GameSession) {
    val player = session.selectedPlayer

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Reveal Equipment", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (player.equipment.isEmpty()) {
                Text("No equipment to reveal.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                player.equipment.forEach { card ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = card.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        OutlinedButton(
                            enabled = card !in player.shownEquipment,
                            onClick = { session.showEquipment(card) }
                        ) {
                            Text(if (card in player.shownEquipment) "Shown" else "Show")
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun StealPanel(session: GameSession) {
    val thief = session.selectedPlayer
    val targets = session.players.filter { it != thief }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Steal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            targets.forEach { target ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${target.name}  Hand ${target.hand.size}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedButton(
                            enabled = target.hand.isNotEmpty(),
                            onClick = { session.stealRandomHandCard(target) }
                        ) {
                            Text("Steal Hand")
                        }
                    }
                    if (target.shownEquipment.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            target.shownEquipment.forEach { card ->
                                OutlinedButton(onClick = { session.stealShownEquipment(target, card) }) {
                                    Text("Steal ${card.name}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatControls(session: GameSession) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("HP", "Max HP", "Energy", "Max Energy", "AR", "EXP").forEach { stat ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stat, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { session.modifyStat(stat, -1) }) {
                        Text("-")
                    }
                    OutlinedButton(onClick = { session.modifyStat(stat, 1) }) {
                        Text("+")
                    }
                }
            }
        }
    }
}

@Composable
private fun CardListSection(
    title: String,
    emptyText: String,
    cards: List<GameCard>,
    primaryAction: String,
    secondaryAction: String?,
    onPrimary: (GameCard) -> Unit,
    onSecondary: (GameCard) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (cards.isEmpty()) {
            Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            cards.forEach { card ->
                CardRow(
                    card = card,
                    primaryAction = primaryAction,
                    secondaryAction = secondaryAction,
                    onPrimary = onPrimary,
                    onSecondary = onSecondary
                )
            }
        }
    }
}

@Composable
private fun CardRow(
    card: GameCard,
    primaryAction: String,
    secondaryAction: String?,
    onPrimary: (GameCard) -> Unit,
    onSecondary: (GameCard) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (card.isPremium) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(card.imageResId),
                contentDescription = card.name,
                modifier = Modifier
                    .width(84.dp)
                    .height(116.dp),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(card.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = card.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onPrimary(card) }) {
                        Text(primaryAction)
                    }
                    if (secondaryAction != null) {
                        OutlinedButton(onClick = { onSecondary(card) }) {
                            Text(secondaryAction)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(session: GameSession) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        SettingSlider(
            title = "Players",
            value = session.players.size,
            valueRange = 2f..5f,
            onValueChange = session::setPlayerCount
        )
        SettingSlider(
            title = "Starting Energy",
            value = session.startingEnergy,
            valueRange = 2f..8f,
            onValueChange = session::updateStartingEnergy
        )
        SettingSwitch(
            title = "Event Mode",
            description = "Tracks whether the black hole event map rules are active.",
            checked = session.eventMode,
            onCheckedChange = { session.eventMode = it }
        )
        SettingSwitch(
            title = "Premium Cards",
            description = "Allows premium deck draws from the main screen.",
            checked = session.showPremiumCards,
            onCheckedChange = { session.showPremiumCards = it }
        )
        HorizontalDivider()
        Button(modifier = Modifier.fillMaxWidth(), onClick = session::resetGame) {
            Text("Reset Session")
        }
        Text(
            text = "This utility version ports the original prototype's card piles, player stats, equipment, discard and draw flow into Android.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingSlider(
    title: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = valueRange,
            steps = (valueRange.endInclusive - valueRange.start).roundToInt() - 1
        )
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private val playerColors = listOf(
    Color(0xFFC62828),
    Color(0xFF2E7D32),
    Color(0xFF1565C0),
    Color(0xFFF9A825),
    Color(0xFF6A1B9A)
)

private fun cardImageResId(cardName: String): Int = when (cardName) {
    "Shoot" -> R.drawable.card_shoot
    "Dodge" -> R.drawable.card_dodge
    "Repair" -> R.drawable.card_repair
    "Signal Interference" -> R.drawable.card_signal_interference
    "Void Phase" -> R.drawable.card_void_phase
    "Breacher Missile" -> R.drawable.card_breacher_missile
    "Hack" -> R.drawable.card_hack
    "Plunder" -> R.drawable.card_plunder
    "Magnetic Tether" -> R.drawable.card_magnetic_tether
    "Drone Swarm" -> R.drawable.card_drone_swarm
    "Standoff" -> R.drawable.card_standoff
    "Ancient Defense Beam" -> R.drawable.card_ancient_defense_beam
    "Harvest Day" -> R.drawable.card_harvest_day
    "Armor-Piercing Rounds" -> R.drawable.card_armor_piercing_rounds
    "Swarm Missiles" -> R.drawable.card_swarm_missiles
    "Fuel Tank" -> R.drawable.card_fuel_tank
    "Reinforced Plating" -> R.drawable.card_reinforced_plating
    "Particle Deflector" -> R.drawable.card_particle_deflector
    "Enhanced Shoot" -> R.drawable.card_enhanced_shoot
    "Enhanced Dodge" -> R.drawable.card_enhanced_dodge
    "Enhanced Repair" -> R.drawable.card_enhanced_repair
    "Energy Drain" -> R.drawable.card_energy_drain
    "Remote Control" -> R.drawable.card_remote_control
    "Recoil Thruster Module" -> R.drawable.card_recoil_thruster_module
    "Supply Crate" -> R.drawable.card_supply_crate
    "Tachyon Lance" -> R.drawable.card_tachyon_lance
    "Focused Arc Emitter" -> R.drawable.card_focused_arc_emitter
    "Reverse Engineering Module" -> R.drawable.card_reverse_engineering_module
    "Energy Dampening Field" -> R.drawable.card_energy_dampening_field
    "Resource Recycler" -> R.drawable.card_resource_recycler
    "Quantum Teleporter" -> R.drawable.card_quantum_teleporter
    "Artifact" -> R.drawable.card_artifact
    else -> R.drawable.card_shoot
}

private val cardTemplates = listOf(
    CardTemplate("Shoot", "Attack a target within your attack range dealing 1 damage. Cost: 1.", false, 26),
    CardTemplate("Dodge", "Nullify the damage of an attack targeted at you. Cost: 0.", false, 12),
    CardTemplate("Repair", "Restore 1 HP during your turn.", false, 7),
    CardTemplate("Signal Interference", "Cause a player to skip their next draw phase. Range: 2. Cost: 1.", false, 2),
    CardTemplate("Void Phase", "Make yourself or another player immune to the next damage instance. Range: 3. Cost: 1.", false, 2),
    CardTemplate("Breacher Missile", "Force a target to discard 1 card. Range: 3. Cost: 1.", false, 6),
    CardTemplate("Hack", "Nullify a played card. Advanced skills require an additional Hack.", false, 7),
    CardTemplate("Plunder", "Steal 1 card and 1 Energy from the target. Range: 1. Cost: 2.", false, 5),
    CardTemplate("Magnetic Tether", "Tether a player to a planet or another player. Moving 5 range away deals 2 damage. Range: 5. Cost: 1.", false, 2),
    CardTemplate("Drone Swarm", "Target a grid. Players within 3 grid range must play Dodge or take 2 damage. Range: 5. Cost: 2.", false, 3),
    CardTemplate("Standoff", "You and a target alternate playing Shoot. First unable to play takes 1 damage.", false, 3),
    CardTemplate("Ancient Defense Beam", "Select two Gaia Worlds and generate a laser between them. Players caught discard Shoot or take 1 damage.", false, 3),
    CardTemplate("Harvest Day", "Reveal cards equal to surviving players. Each player selects 1 revealed card in turn order.", false, 2),
    CardTemplate("Armor-Piercing Rounds", "Equipment: Attack Range +1; attacks ignore target equipment effects.", false, 1),
    CardTemplate("Swarm Missiles", "Equipment: Attack Range -1; Shoot energy cost -1.", false, 1),
    CardTemplate("Fuel Tank", "Gain 2 Energy.", false, 2),
    CardTemplate("Reinforced Plating", "Equipment: Skill damage received -1.", false, 1),
    CardTemplate("Particle Deflector", "Equipment: Nullify damage and deal it back to the source, then discard this card.", false, 1),
    CardTemplate("Enhanced Shoot", "Attack a target twice within infinite range, dealing 1 damage each.", true, 2),
    CardTemplate("Enhanced Dodge", "Nullify attack damage and force the attacker to discard 2 cards.", true, 2),
    CardTemplate("Enhanced Repair", "Restore 2 HP and 1 Energy to yourself.", true, 2),
    CardTemplate("Energy Drain", "Deplete a player's energy to 0 next turn. Range: 2. Cost: 2.", true, 1),
    CardTemplate("Remote Control", "After dealing damage, move the target by 3 grids. Cost: 0.", true, 1),
    CardTemplate("Recoil Thruster Module", "Equipment: You may use Shoot as Dodge, and Dodge as Shoot.", true, 1),
    CardTemplate("Supply Crate", "Draw 1 Premium card or 3 Standard cards.", true, 1),
    CardTemplate("Tachyon Lance", "Equipment: Attack Range +4; Shoot energy cost +1.", true, 1),
    CardTemplate("Focused Arc Emitter", "Equipment: Attack Range +2; discard 1 card to make a dodged Shoot hit for +1 damage.", true, 1),
    CardTemplate("Reverse Engineering Module", "Equipment: When damaged, you may obtain the card that damaged you.", true, 1),
    CardTemplate("Energy Dampening Field", "Equipment: Damage from more than 1 range away is reduced by 1.", true, 1),
    CardTemplate("Resource Recycler", "Equipment: Once per turn, discard any number of cards and draw the same number.", true, 1),
    CardTemplate("Quantum Teleporter", "Equipment: Skill Range +2.", true, 1),
    CardTemplate("Artifact", "Energy is emptied when obtained. Reach the border with this card in hand to win.", true, 1)
)

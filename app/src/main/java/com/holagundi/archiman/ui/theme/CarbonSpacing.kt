package com.holagundi.archiman.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * IBM Carbon spacing scale (2px base). Prefer these tokens over ad-hoc dp values.
 * @see https://carbondesignsystem.com/elements/spacing/overview/
 */
object CarbonSpacing {
    val spacing01: Dp = 2.dp
    val spacing02: Dp = 4.dp
    val spacing03: Dp = 8.dp
    val spacing04: Dp = 12.dp
    val spacing05: Dp = 16.dp
    val spacing06: Dp = 24.dp
    val spacing07: Dp = 32.dp
    val spacing08: Dp = 40.dp
    val spacing09: Dp = 48.dp
    val spacing10: Dp = 64.dp
    val spacing11: Dp = 80.dp
    val spacing12: Dp = 96.dp
    val spacing13: Dp = 160.dp

    /** Horizontal page gutter / screen margin. */
    val screen: Dp = spacing05

    /** Vertical rhythm between major sections. */
    val section: Dp = spacing05

    /** Default stack between cards / list blocks. */
    val stack: Dp = spacing04

    /** Tight stack inside a card or dense list. */
    val stackTight: Dp = spacing03

    /** Micro gap (labels over values, icon to text). */
    val micro: Dp = spacing02

    /** Hairline gap (paired lines). */
    val hairline: Dp = spacing01

    /** Standard card content padding. */
    val card: Dp = spacing05

    /** Dense card / disclosure body padding. */
    val cardDense: Dp = spacing04

    /** FAB / bottom nav clearance for scroll content. */
    val scrollBottom: Dp = spacing06

    val screenPadding: PaddingValues
        get() = PaddingValues(horizontal = screen, vertical = stack)

    val screenPaddingBottomHeavy: PaddingValues
        get() = PaddingValues(start = screen, end = screen, top = stack, bottom = scrollBottom)

    val sectionArrangement: Arrangement.HorizontalOrVertical
        get() = Arrangement.spacedBy(section)

    val stackArrangement: Arrangement.HorizontalOrVertical
        get() = Arrangement.spacedBy(stack)

    val tightArrangement: Arrangement.HorizontalOrVertical
        get() = Arrangement.spacedBy(stackTight)
}

fun Modifier.carbonScreenPadding(): Modifier =
    padding(horizontal = CarbonSpacing.screen, vertical = CarbonSpacing.stack)

fun Modifier.carbonCardPadding(): Modifier =
    padding(CarbonSpacing.card)

fun Modifier.carbonCardDensePadding(): Modifier =
    padding(CarbonSpacing.cardDense)

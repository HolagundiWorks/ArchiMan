package com.holagundi.archiman.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.*
import androidx.compose.material3.Button as MaterialButton
import androidx.compose.material3.OutlinedButton as MaterialOutlinedButton
import androidx.compose.material3.TextButton as MaterialTextButton
import androidx.compose.material3.FilledTonalButton as MaterialTonalButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable fun CarbonButton(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,shape:Shape=RectangleShape,colors:ButtonColors=ButtonDefaults.buttonColors(),elevation:ButtonElevation?=null,border:BorderStroke?=null,contentPadding:PaddingValues=ButtonDefaults.ContentPadding,interactionSource:MutableInteractionSource?=null,content:@Composable RowScope.()->Unit) {
    MaterialButton(onClick,modifier.heightIn(min=48.dp),enabled,shape,colors,elevation,border,contentPadding,interactionSource,content)
}
@Composable fun CarbonOutlinedButton(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,shape:Shape=RectangleShape,colors:ButtonColors=ButtonDefaults.outlinedButtonColors(),elevation:ButtonElevation?=null,border:BorderStroke?=ButtonDefaults.outlinedButtonBorder(enabled),contentPadding:PaddingValues=ButtonDefaults.ContentPadding,interactionSource:MutableInteractionSource?=null,content:@Composable RowScope.()->Unit) {
    MaterialOutlinedButton(onClick,modifier.heightIn(min=48.dp),enabled,shape,colors,elevation,border,contentPadding,interactionSource,content)
}
@Composable fun CarbonTextButton(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,shape:Shape=RectangleShape,colors:ButtonColors=ButtonDefaults.textButtonColors(),elevation:ButtonElevation?=null,border:BorderStroke?=null,contentPadding:PaddingValues=ButtonDefaults.TextButtonContentPadding,interactionSource:MutableInteractionSource?=null,content:@Composable RowScope.()->Unit) {
    MaterialTextButton(onClick,modifier.heightIn(min=48.dp),enabled,shape,colors,elevation,border,contentPadding,interactionSource,content)
}
@Composable fun CarbonTonalButton(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,shape:Shape=RectangleShape,colors:ButtonColors=ButtonDefaults.filledTonalButtonColors(),elevation:ButtonElevation?=null,border:BorderStroke?=null,contentPadding:PaddingValues=ButtonDefaults.ContentPadding,interactionSource:MutableInteractionSource?=null,content:@Composable RowScope.()->Unit) {
    MaterialTonalButton(onClick,modifier.heightIn(min=48.dp),enabled,shape,colors,elevation,border,contentPadding,interactionSource,content)
}

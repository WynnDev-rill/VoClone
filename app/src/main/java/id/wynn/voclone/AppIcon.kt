package id.wynn.voclone

import androidx.annotation.DrawableRes
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource

/** Selected official Material vectors; avoids packaging/dexing the entire icon catalogue. */
@Composable fun AppIcon(@DrawableRes icon: Int,contentDescription: String?,modifier: Modifier=Modifier,tint: Color=LocalContentColor.current) {
    androidx.compose.material3.Icon(painter=painterResource(icon),contentDescription=contentDescription,modifier=modifier,tint=tint)
}

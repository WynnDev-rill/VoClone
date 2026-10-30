package id.wynn.voclone

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.app.Activity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Jakarta=FontFamily(Font(R.font.plus_jakarta_sans))
private val Light=lightColorScheme(primary=Color(0xFF1764DF),onPrimary=Color.White,primaryContainer=Color(0xFFE5EFFF),onPrimaryContainer=Color(0xFF103E82),background=Color(0xFFF8FAFF),surface=Color.White,onSurface=Color(0xFF101B35),onBackground=Color(0xFF101B35),onSurfaceVariant=Color(0xFF53617A),outline=Color(0xFF71809A),outlineVariant=Color(0xFFC4CFE1),surfaceContainer=Color(0xFFF0F5FE),surfaceContainerLow=Color(0xFFF5F8FF))
private val Dark=darkColorScheme(primary=Color(0xFFA8C7FF),onPrimary=Color(0xFF003062),primaryContainer=Color(0xFF163E73),onPrimaryContainer=Color(0xFFD8E7FF),background=Color(0xFF0D1422),surface=Color(0xFF172033),onSurface=Color(0xFFE6EDFA),onBackground=Color(0xFFE6EDFA),onSurfaceVariant=Color(0xFFBAC7DD),outline=Color(0xFF8292AC),outlineVariant=Color(0xFF3B4961),surfaceContainer=Color(0xFF1C2A42),surfaceContainerLow=Color(0xFF131E30))
@Composable fun VoCloneTheme(appearance: String,content: @Composable ()->Unit) {
    val dark=appearance=="dark" || (appearance=="system" && isSystemInDarkTheme())
    val view=LocalView.current
    SideEffect { (view.context as? Activity)?.let { activity ->
        WindowCompat.getInsetsController(activity.window,view).apply { isAppearanceLightStatusBars=!dark;isAppearanceLightNavigationBars=!dark }
    } }
    val defaults=Typography()
    fun style(size: Int,line: Int,weight: FontWeight=FontWeight.Normal)=TextStyle(fontFamily=Jakarta,fontSize=size.sp,lineHeight=line.sp,fontWeight=weight)
    val typography=defaults.copy(headlineLarge=style(34,42,FontWeight.Bold),headlineSmall=style(24,32,FontWeight.Bold),titleLarge=style(22,30,FontWeight.Bold),titleMedium=style(16,24,FontWeight.SemiBold),titleSmall=style(14,22,FontWeight.SemiBold),bodyLarge=style(16,24),bodyMedium=style(14,22),bodySmall=style(12,18),labelLarge=style(14,20,FontWeight.SemiBold),labelMedium=style(12,18,FontWeight.Medium))
    MaterialTheme(colorScheme=if(dark)Dark else Light,typography=typography,content=content)
}

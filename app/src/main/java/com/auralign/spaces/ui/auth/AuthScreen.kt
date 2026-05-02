package com.auralign.spaces.ui.auth

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.auralign.spaces.ui.theme.BrandAccent
import com.auralign.spaces.ui.theme.BrandPrimary
import com.auralign.spaces.ui.theme.BrandSecondary
import com.auralign.spaces.ui.theme.BrandTertiary
import com.auralign.spaces.ui.theme.SplashGradient

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val isSignIn by viewModel.isSignInTab.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) onAuthSuccess()
    }

    val cs = MaterialTheme.colorScheme

    Box(modifier = Modifier.fillMaxSize().background(cs.background)) {

        // ── Top brand strip ──────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.38f)
                .background(SplashGradient),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.ViewInAr, null, tint = Color.White, modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text("Auralign", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text("SPACES", color = Color.White.copy(0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 5.sp)
            }
        }

        // ── White card for form ───────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.72f)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(cs.surface)
                .padding(horizontal = 28.dp)
                .padding(top = 32.dp, bottom = 24.dp)
        ) {
            // Tab switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(cs.surfaceVariant)
                    .padding(4.dp)
            ) {
                TabPill("Sign In",  isSignIn,  Modifier.weight(1f)) { viewModel.setTab(true) }
                TabPill("Register", !isSignIn, Modifier.weight(1f)) { viewModel.setTab(false) }
            }

            Spacer(Modifier.height(28.dp))

            Crossfade(targetState = isSignIn, animationSpec = tween(300)) { signin ->
                if (signin) SignInForm(viewModel, authState, cs)
                else        SignUpForm(viewModel, authState, cs)
            }

            if (authState is AuthState.Error) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = (authState as AuthState.Error).message,
                    color = cs.error,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(24.dp))

            // Google SSO
            OutlinedButton(
                onClick = { /* Google auth */ },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.onSurface),
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Icon(Icons.Rounded.Link, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Continue with Google", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun TabPill(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(250)
    )
    val textColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(250)
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
private fun SignInForm(viewModel: AuthViewModel, authState: AuthState, cs: ColorScheme) {
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CleanInput(email, { email = it }, "Email address", Icons.Rounded.AlternateEmail)
        CleanInput(password, { password = it }, "Password", Icons.Rounded.Lock, isPassword = true)
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = { viewModel.signInWithEmail(email, password) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = cs.primary, contentColor = cs.onPrimary)
        ) {
            if (authState is AuthState.Loading)
                CircularProgressIndicator(color = cs.onPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else
                Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun SignUpForm(viewModel: AuthViewModel, authState: AuthState, cs: ColorScheme) {
    var name     by remember { mutableStateOf("") }
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CleanInput(name,     { name = it },     "Full name",      Icons.Rounded.Person)
        CleanInput(email,    { email = it },    "Email address",  Icons.Rounded.AlternateEmail)
        CleanInput(password, { password = it }, "Password",       Icons.Rounded.Lock, isPassword = true)
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = { viewModel.register(name, email, password) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = cs.primary, contentColor = cs.onPrimary)
        ) {
            if (authState is AuthState.Loading)
                CircularProgressIndicator(color = cs.onPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else
                Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CleanInput(
    value: String, onValueChange: (String) -> Unit,
    label: String, icon: ImageVector, isPassword: Boolean = false
) {
    var visible by remember { mutableStateOf(!isPassword) }
    val cs = MaterialTheme.colorScheme

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 14.sp) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(20.dp)) },
        trailingIcon = if (isPassword) {{
            IconButton(onClick = { visible = !visible }) {
                Icon(if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null)
            }
        }} else null,
        visualTransformation = if (!visible) PasswordVisualTransformation() else VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor    = cs.primary,
            unfocusedBorderColor  = cs.outline,
            focusedLabelColor     = cs.primary,
            unfocusedLabelColor   = cs.onSurfaceVariant,
            focusedTextColor      = cs.onSurface,
            unfocusedTextColor    = cs.onSurface,
            focusedLeadingIconColor   = cs.primary,
            unfocusedLeadingIconColor = cs.onSurfaceVariant
        )
    )
}

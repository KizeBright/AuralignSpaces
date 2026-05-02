package com.auralign.spaces.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.auralign.spaces.ui.theme.SplashGradient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state  by viewModel.state.collectAsState()
    val isDark by viewModel.isDarkMode.collectAsState()
    val cs     = MaterialTheme.colorScheme

    Scaffold(
        containerColor = cs.background,
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold, color = cs.onBackground) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(16.dp))

            // ── Avatar ───────────────────────────────────────────
            Box(
                modifier = Modifier.size(110.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(SplashGradient)
                        .border(4.dp, cs.surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.name.trim().take(1).uppercase().ifEmpty { "?" },
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 44.sp
                    )
                }
                
                // Edit / Camera button
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(cs.primary)
                        .border(2.dp, cs.surface, CircleShape)
                        .clickable { /* Photo picker logic */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.CameraAlt,
                        null,
                        tint = cs.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(state.name, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = cs.onBackground)
            Text(state.email, fontSize = 14.sp, color = cs.onSurfaceVariant)

            Spacer(Modifier.height(32.dp))

            // ── Info card ────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = cs.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    ProfileInfoRow(label = "Email", value = state.email, icon = Icons.Rounded.Email)
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = cs.outlineVariant)

                    if (state.isEditing) {
                        Text("Full Name", fontSize = 12.sp, color = cs.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = state.name,
                            onValueChange = { viewModel.updateName(it) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = cs.primary,
                                unfocusedBorderColor = cs.outline,
                                focusedTextColor     = cs.onSurface,
                                unfocusedTextColor   = cs.onSurface
                            )
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { viewModel.toggleEdit() }) {
                                Text("Cancel", color = cs.onSurfaceVariant)
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.saveProfile() },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
                            ) {
                                Text("Save")
                            }
                        }
                    } else {
                        ProfileInfoRow(
                            label = "Full Name",
                            value = state.name,
                            icon  = Icons.Rounded.Person,
                            trailing = {
                                IconButton(onClick = { viewModel.toggleEdit() }) {
                                    Icon(Icons.Rounded.Edit, null, tint = cs.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        )
                    }

                    if (state.error != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(state.error!!, color = cs.error, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Settings card ────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = cs.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.DarkMode, null, tint = cs.primary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Dark Mode", fontWeight = FontWeight.Medium, color = cs.onSurface)
                        Text("Switch to dark theme", fontSize = 12.sp, color = cs.onSurfaceVariant)
                    }
                    Switch(
                        checked = isDark,
                        onCheckedChange = { viewModel.toggleDarkMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor  = cs.onPrimary,
                            checkedTrackColor  = cs.primary,
                            uncheckedThumbColor = cs.onSurfaceVariant,
                            uncheckedTrackColor = cs.surfaceVariant
                        )
                    )
                }
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(32.dp))

            // ── Logout ───────────────────────────────────────────
            OutlinedButton(
                onClick = { viewModel.signOut(); onSignOut() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.error),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
            ) {
                Icon(Icons.Rounded.Logout, null, tint = cs.error, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Sign Out", color = cs.error, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProfileInfoRow(
    label: String,
    value: String,
    icon: ImageVector,
    trailing: @Composable (() -> Unit)? = null
) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = cs.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 12.sp, color = cs.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.Medium, color = cs.onSurface, fontSize = 15.sp)
        }
        trailing?.invoke()
    }
}

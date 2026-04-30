package com.biospace.monitor.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.*
import com.biospace.monitor.ui.MainViewModel
import com.biospace.monitor.ui.theme.*

@Composable
fun ChatScreen(vm: MainViewModel) {
    val messages  by vm.chatMessages.collectAsState()
    val chatInput by vm.chatInput.collectAsState()
    val username  by vm.username.collectAsState()
    val sw     by vm.sw.collectAsState()
    val burden by vm.burden.collectAsState()
    var showNameDialog by remember { mutableStateOf(username.isBlank()) }
    var tempName by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size-1) }

    if (showNameDialog) {
        AlertDialog(onDismissRequest={}, containerColor=CardBg,
            title={ Text("SET USERNAME", color=CyanColor, fontFamily=FontFamily.Monospace, fontSize=12.sp) },
            text={ OutlinedTextField(value=tempName, onValueChange={tempName=it},
                placeholder={Text("Anonymous",color=DimColor,fontFamily=FontFamily.Monospace)},
                colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=CyanColor,unfocusedBorderColor=BorderColor,
                    focusedTextColor=TextColor,unfocusedTextColor=TextColor,containerColor=CardBg,cursorColor=CyanColor)) },
            confirmButton={ TextButton(onClick={vm.setUsername(tempName.ifBlank{"Anonymous"}); showNameDialog=false}) {
                Text("JOIN CHAT",color=CyanColor,fontFamily=FontFamily.Monospace) } })
    }

    Column(Modifier.fillMaxSize().background(BgColor)) {
        Column(Modifier.padding(16.dp,12.dp,16.dp,8.dp)) {
            Text("COMMUNITY CHAT", color=CyanColor, fontSize=11.sp, letterSpacing=3.sp, fontFamily=FontFamily.Monospace)
            Text("PUBLIC · LIVE · Kp ${sw.kp} · BURDEN ${burden.overall.toInt()}%", color=DimColor, fontSize=8.sp, fontFamily=FontFamily.Monospace)
            Text("Every user's current Kp and burden score is shown on their messages.", color=DimColor, fontSize=8.sp, fontFamily=FontFamily.Monospace)
        }
        LazyColumn(state=listState, modifier=Modifier.weight(1f).padding(horizontal=12.dp),
            verticalArrangement=Arrangement.spacedBy(6.dp), contentPadding=PaddingValues(vertical=8.dp)) {
            items(messages, key={it.id}) { msg ->
                val isMe = msg.username == username
                Row(Modifier.fillMaxWidth(), horizontalArrangement=if(isMe) Arrangement.End else Arrangement.Start) {
                    Column(horizontalAlignment=if(isMe) Alignment.End else Alignment.Start,
                        modifier=Modifier.widthIn(max=300.dp)) {
                        Row(horizontalArrangement=Arrangement.spacedBy(6.dp), verticalAlignment=Alignment.CenterVertically) {
                            Text(msg.username, color=if(isMe) CyanColor else AmberColor, fontSize=8.sp, fontFamily=FontFamily.Monospace)
                            Text("Kp${msg.kp} B${msg.burden.toInt()}%", color=DimColor, fontSize=7.sp, fontFamily=FontFamily.Monospace)
                        }
                        Box(Modifier.clip(RoundedCornerShape(topStart=12.dp,topEnd=12.dp,
                            bottomStart=if(isMe)12.dp else 2.dp, bottomEnd=if(isMe)2.dp else 12.dp))
                            .background(if(isMe) CyanColor.copy(0.15f) else CardBg)
                            .border(1.dp,if(isMe)CyanColor.copy(0.3f) else BorderColor,RoundedCornerShape(12.dp)).padding(10.dp)) {
                            Text(msg.message, color=TextColor, fontSize=12.sp, fontFamily=FontFamily.Monospace)
                        }
                        Text(java.text.SimpleDateFormat("HH:mm").format(java.util.Date(msg.timestamp)),
                            color=DimColor, fontSize=7.sp, fontFamily=FontFamily.Monospace)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().background(CardBg).padding(8.dp), verticalAlignment=Alignment.CenterVertically) {
            OutlinedTextField(value=chatInput, onValueChange={vm.setChatInput(it)},
                placeholder={Text("Share what you're feeling...",color=DimColor,fontFamily=FontFamily.Monospace,fontSize=11.sp)},
                modifier=Modifier.weight(1f), singleLine=true,
                colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=CyanColor,unfocusedBorderColor=BorderColor,
                    focusedTextColor=TextColor,unfocusedTextColor=TextColor,containerColor=BgColor,cursorColor=CyanColor))
            Spacer(Modifier.width(8.dp))
            IconButton(onClick={vm.sendChatMessage()},
                colors=IconButtonDefaults.iconButtonColors(containerColor=CyanColor.copy(0.2f))) {
                Icon(Icons.Filled.Send, contentDescription="Send", tint=CyanColor)
            }
        }
    }
}

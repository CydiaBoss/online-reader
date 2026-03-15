package com.wang.twkanviewer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wang.twkanviewer.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(
    useExternalTranslator: Boolean,
    onUseExternalTranslatorChange: (Boolean) -> Unit,
    translatorApiKey: String,
    onTranslatorApiKeyChange: (String) -> Unit,
    userAgent: String,
    onUserAgentChange: (String) -> Unit,
    chapterFontSize: Float,
    onChapterFontSizeChange: (Float) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        bottomBar = {
            BottomAppBar {
                IconButton(onClick = onBackClick, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_content_description))
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            // "Settings" title at the top of the content area with minimal top padding
            Text(
                stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            // Translator Settings
            Text(stringResource(R.string.translation_section), style = MaterialTheme.typography.titleMedium)
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.use_external_translator_label), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.use_external_translator_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = useExternalTranslator,
                    onCheckedChange = onUseExternalTranslatorChange
                )
            }

            if (useExternalTranslator) {
                OutlinedTextField(
                    value = translatorApiKey,
                    onValueChange = onTranslatorApiKeyChange,
                    label = { Text(stringResource(R.string.translator_api_key_label)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    singleLine = true
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

            // Browser Settings
            Text(stringResource(R.string.browser_section), style = MaterialTheme.typography.titleMedium)
            
            OutlinedTextField(
                value = userAgent,
                onValueChange = onUserAgentChange,
                label = { Text(stringResource(R.string.user_agent_label)) },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                placeholder = { Text(stringResource(R.string.user_agent_placeholder)) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

            // Reader Settings
            Text(stringResource(R.string.reader_section), style = MaterialTheme.typography.titleMedium)
            
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(stringResource(R.string.chapter_font_size_label, chapterFontSize.toInt()), style = MaterialTheme.typography.bodyLarge)
                Slider(
                    value = chapterFontSize,
                    onValueChange = onChapterFontSizeChange,
                    valueRange = 12f..32f,
                    steps = 20
                )
            }
        }
    }
}

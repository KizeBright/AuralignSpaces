import os
import glob

def refactor():
    replacements = {
        "AuralignColors.Obsidian": "MaterialTheme.colorScheme.background",
        "AuralignColors.Midnight": "MaterialTheme.colorScheme.surface",
        "AuralignColors.SurfaceCard": "MaterialTheme.colorScheme.surfaceVariant",
        "AuralignColors.Surface": "MaterialTheme.colorScheme.surface",
        "AuralignColors.Border": "MaterialTheme.colorScheme.outline",
        "AuralignColors.Electric": "MaterialTheme.colorScheme.primary",
        "AuralignColors.Emerland": "MaterialTheme.colorScheme.tertiary",
        "AuralignColors.Emerald": "MaterialTheme.colorScheme.tertiary",
        "AuralignColors.Violet": "MaterialTheme.colorScheme.secondary",
        "AuralignColors.Rose": "MaterialTheme.colorScheme.error",
        "AuralignColors.Amber": "MaterialTheme.colorScheme.tertiaryContainer",
        "AuralignColors.TextPrimary": "MaterialTheme.colorScheme.onBackground",
        "AuralignColors.TextSecondary": "MaterialTheme.colorScheme.onSurfaceVariant",
        "AuralignColors.TextMuted": "MaterialTheme.colorScheme.outlineVariant",
        "com.auralign.spaces.ui.theme.AuralignColors.": "",
        "com.auralign.spaces.ui.theme.AuralignColors": ""
    }

    files = glob.glob("app/src/main/java/com/auralign/spaces/**/*.kt", recursive=True)
    for f in files:
        if "Color.kt" in f or "Theme.kt" in f:
            continue
        with open(f, "r") as file:
            content = file.read()
        
        orig_content = content
        for k, v in replacements.items():
            content = content.replace(k, v)
        
        if content != orig_content:
            if "MaterialTheme" in content and "import androidx.compose.material3.MaterialTheme" not in content:
                # Add import if needed
                lines = content.split('\n')
                for i, line in enumerate(lines):
                    if line.startswith("import "):
                        lines.insert(i, "import androidx.compose.material3.MaterialTheme")
                        break
                content = '\n'.join(lines)
            
            with open(f, "w") as file:
                file.write(content)

if __name__ == "__main__":
    refactor()

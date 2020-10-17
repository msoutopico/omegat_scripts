SGKoishi's edit, to replace with source if source text matches pattern:

```
import java.util.regex.Pattern
def pattern = /^\{\$[a-zA-Z0-9\.]*\}$/
if (target == null && (source =~ pattern).matches()) { ... }
```

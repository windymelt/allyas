# allyas

A command aliasing tool using symlinks and S-expression configuration.

## SYNOPSIS

**allyas** [*command*]

## DESCRIPTION

**allyas** provides command aliasing through symlinks. Commands are defined in an S-expression configuration file and executed when the binary is invoked under different names.

The tool operates in three modes:

- **allyas**: Display help
- **shim**: Create symlinks for all configured aliases  
- *alias*: Execute configured command (any other name)

## CONFIGURATION

Set the **ALLYAS_CONF** environment variable to specify the configuration file path.

Configuration uses S-expression syntax:

```lisp
(config
  (alias "ll" "ls -la")
  (alias "gs" "git status")
  (alias "gc" "git commit"))
```

## ENVIRONMENT

**ALLYAS_CONF**
: Path to configuration file (required)

**ALLYAS_SHIM_DIR**  
: Directory for symlink creation (required for automatic symlink creation)

**ALLYAS_VERBOSE**
: Enable verbose output when set to "true"

**SHELL**
: Shell to use for command execution (default: sh)

## USAGE

1. Create configuration file:
   ```bash
   export ALLYAS_CONF=/path/to/config.conf
   ```

2. Place executable in your `$PATH`:
   ```bash
   mv allyas ~/bin/ # if you have ~/bin in your $PATH
   ```

3. Create symlinks:
   ```bash
   export ALLYAS_SHIM_DIR=/path/to/shims
   allyas shim
   ```

4. Use aliases:
   ```bash
   ll /etc
   gs
   gc -m "commit message"
   ```

## EXIT STATUS

**0**: Success  
**1**: Configuration error or unknown command  
**127**: Command not found  

Other exit codes are propagated from executed commands.

## EXAMPLES

Configuration with arguments:
```lisp
(config
  (alias "build" "pnpm run build")
  (alias "docker-ps" "docker ps --format table"))
```

Execution passes additional arguments:
```bash
build         # Executes: pnpm run build
docker-ps -a  # Executes: docker ps --format table '-a'
```

## WORKS WELL WITH DIRENV(1)

It works well with [direnv](https://direnv.net).

Configure `.envrc` like this:

```sh
export ALLYAS_CONF=$(pwd)/.allyconf
export ALLYAS_SHIM_DIR=$(pwd)/.aliases
PATH=${PATH}:$ALLYAS_SHIM_DIR
```

Then prepare configuration file:
```lisp
(config
  ...)
```

Finally set symlink and it works well:

```sh
allyas shim
```

## BUILDING

Requires Scala Native. Use **sbt** for all build operations:

```bash
sbt compile          # Compile source
sbt test             # Run tests  
sbt nativeLink       # Build executable
```

# Releasing to Maven Central

This document describes how to publish a new version to Maven Central.

## Prerequisites

1. **Sonatype account** — Register at [central.sonatype.com](https://central.sonatype.com) if you haven't already. You need publishing rights for the `com.yanimetaxas` group ID.

2. **GPG key** — Required for signing artifacts.
   ```bash
   gpg --gen-key
   gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
   ```

3. **Maven settings** — Add your credentials to `~/.m2/settings.xml`:
   ```xml
   <settings>
     <servers>
       <server>
         <id>central</id>
         <username>YOUR_SONATYPE_USERNAME</username>
         <password>YOUR_SONATYPE_TOKEN</password>
       </server>
     </servers>
   </settings>
   ```
   Generate a user token at [central.sonatype.com](https://central.sonatype.com) → Account → Generate User Token.

4. **Codecov token** (optional, for CI coverage) — Add `CODECOV_TOKEN` as a GitHub repository secret. Get the token from [codecov.io](https://codecov.io) after linking the repository.

## Release from a version tag

Pushing a `v*` tag runs [`.github/workflows/release.yml`](.github/workflows/release.yml). The workflow sets the Maven version from the tag, runs the full verification, signs the artifacts, and publishes these modules to Maven Central:

- `bookkeeping-core`
- `bookkeeping-jdbc`
- `bookkeeping-spring-boot-starter`

`bookkeeping-examples` and `bookkeeping-coverage` are not published. The workflow then creates a GitHub Release for that tag and attaches the three published JARs.

Add these repository secrets before the first tag:

| Secret | Purpose |
|---|---|
| `MAVEN_CENTRAL_USERNAME` | Sonatype user-token username |
| `MAVEN_CENTRAL_TOKEN` | Sonatype user-token password |
| `MAVEN_GPG_PRIVATE_KEY` | Armored private key (`gpg --export-secret-keys --armor KEY_ID`) |
| `MAVEN_GPG_PASSPHRASE` | Passphrase for that key |

```bash
git tag v1.0.0
git push origin v1.0.0
```

## Manual release

### 1. Verify the build

```bash
mvn -T 1C clean verify -P sign
```

This runs all tests, generates sources/javadoc JARs, signs artifacts with GPG, and generates a JaCoCo coverage report. Fix any issues before proceeding.

### 2. Set the release version

Update `pom.xml` with the release version (remove `-SNAPSHOT` if present):
```xml
<version>1.0.0</version>
```

### 3. Deploy to Maven Central

```bash
mvn clean deploy -P sign,release
```

This will:
- Compile, test, and package the project
- Generate sources and javadoc JARs
- Sign all artifacts with GPG
- Upload to Maven Central via the `central-publishing-maven-plugin`

### 4. Verify on Maven Central

After deployment, check that the artifact appears at:
- [central.sonatype.com/artifact/com.yanimetaxas/bookkeeping-core](https://central.sonatype.com/artifact/com.yanimetaxas/bookkeeping-core)

It may take 10-30 minutes for the artifact to sync to Maven Central search.

### 5. Create a GitHub Release

```bash
git tag v1.0.0
git push origin v1.0.0
```

Then create a release on GitHub at: `https://github.com/imetaxas/double-entry-bookkeeping-api/releases/new`

Select the tag, write release notes (reference `CHANGELOG.md`), and publish.

### 6. Post-release

- Update `pom.xml` to the next snapshot version:
  ```xml
  <version>1.0.1-SNAPSHOT</version>
  ```
- Commit and push.

## Using Maven Release Plugin (Alternative)

If you prefer automated version management:

```bash
# Prepare: bumps version, commits, tags
mvn release:clean release:prepare

# Perform: checks out the tag, builds, and deploys
mvn release:perform -Darguments="-P sign,release"
```

If prepare fails:
```bash
mvn release:rollback
mvn release:clean
```

## Troubleshooting

**GPG signing fails:**
```bash
export GPG_TTY=$(tty)  # Fix for GPG passphrase prompt
gpg --list-keys        # Verify key exists
```

**401 Unauthorized on deploy:**
- Verify your `~/.m2/settings.xml` has the correct `<server>` entry with id `central`
- Regenerate your user token at central.sonatype.com

**Artifact not appearing on Maven Central:**
- Wait 10-30 minutes for sync
- Check the deployment at [central.sonatype.com](https://central.sonatype.com) → Deployments

## Legacy OSSRH Publishing

If you previously published via the legacy `oss.sonatype.org` and need to migrate, see: [Sonatype Central Migration Guide](https://central.sonatype.org/publish/publish-portal-guide/).

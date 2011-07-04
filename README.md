# Zi

Zi is a maven plugin for clojure.

This is alpha quality.

## Available goals

 * zi:ritz

## Install

To globally enable the zi plugin, you need to add `pluginGroup` and
`pluginRepository` configuration to your `~/.m2/settings.xml` file.

```xml
    <pluginGroups>
      <pluginGroup>org.cloudhoist.plugin</pluginGroup>
    </pluginGroups>
```

```xml
    <profiles>
      <profile>
        <id>clojure-dev</id>
        <pluginRepositories>
          <pluginRepository>
            <id>sonatype-snapshots</id>
            <url>http://oss.sonatype.org/content/repositories/releases</url>
          </pluginRepository>
        </pluginRepositories>
      </profile>
    </profiles>

    <activeProfiles>
      <activeProfile>clojure-dev</activeProfile>
    </activeProfiles>
```

## Configuration

Currently it is not possible to configure much.  The source paths are fixed
to `src/main/clojure` and `src/test/clojure`.

## Goals

### ritz

The ritz goal starts a ritz server.

<table>
  <tr>
    <th>Property</th>
    <th>Variable</th>
    <th>Default</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>port</td>
    <td>clojure.swank.port</td>
    <td>4005</td>
    <td>The swank server port</td>
  </tr>
  <tr>
    <td>encoding</td>
    <td>clojure.swank.encoding</td>
    <td>iso-8859-1</td>
    <td>The swank encoding to use</td>
  </tr>
</table>


## Zi, the builder

Zi was a builder in [northern mythology](http://www.pitt.edu/~dash/mbuilder.html#eckwadt).

## License

Licensed under [EPL](http://www.eclipse.org/legal/epl-v10.html)

Copyright 2011 Hugo Duncan.

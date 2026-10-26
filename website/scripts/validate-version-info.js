const fs = require("fs");
const path = require("path");

const DEFAULT_FILE = path.join(
    __dirname,
    "..",
    "_site",
    "kst4ContestVersionInfo.xml"
);

function fail(message) {
    throw new Error(`[versionInfo validation] ${message}`);
}

function normaliseVersion(value) {
    return String(value || "")
        .trim()
        .replace(/^v/, "")
        .split("-")[0]
        .split("+")[0];
}

function extractTag(xml, tagName) {
    const match = xml.match(
        new RegExp(`<${tagName}>([\\s\\S]*?)<\\/${tagName}>`)
    );

    return match ? match[1].trim() : null;
}

// Applies the removal until nothing changes, so that nested input such as
// "<!-<!---->-->" cannot leave a fresh comment behind after a single pass.
function removeUntilStable(value, pattern) {
    let previous;
    let current = value;

    do {
        previous = current;
        current = current.replace(pattern, "");
    } while (current !== previous);

    return current;
}

function assertWellFormedStructure(xml) {
    if (/&(?!amp;|lt;|gt;|quot;|apos;|#\d+;|#x[0-9a-f]+;)/i.test(xml)) {
        fail("the XML contains an unescaped ampersand");
    }

    const withoutCommentsAndDeclaration = removeUntilStable(
        xml,
        /<!--[\s\S]*?-->|<\?[\s\S]*?\?>/g
    );

    const tagPattern =
        /<\/?([A-Za-z_][\w:.-]*)(?:\s[^<>]*?)?\s*\/?>/g;

    const stack = [];
    let match;

    while (
        (match = tagPattern.exec(withoutCommentsAndDeclaration)) !== null
        ) {
        const fullTag = match[0];
        const tagName = match[1];

        if (fullTag.startsWith("</")) {
            const openTag = stack.pop();

            if (openTag !== tagName) {
                fail(
                    `closing tag </${tagName}> does not match `
                    + `<${openTag || "none"}>`
                );
            }
        } else if (!fullTag.endsWith("/>")) {
            stack.push(tagName);
        }
    }

    if (stack.length > 0) {
        fail(`unclosed tag <${stack[stack.length - 1]}>`);
    }

    const remainingMarkup =
        withoutCommentsAndDeclaration.replace(tagPattern, "");

    // A plain ">" is legal character data and occurs in historical
    // changelog notation such as "->". A remaining "<" cannot be legal
    // after all valid tags have been removed.
    if (/</.test(remainingMarkup)) {
        fail("the XML contains malformed markup");
    }
}

function assertContainsTag(block, tagName) {
    const pattern =
        new RegExp(`<${tagName}>[\\s\\S]*?<\\/${tagName}>`);

    if (!pattern.test(block)) {
        fail(`required element <${tagName}> is missing`);
    }
}

function validateVersionInfo(xml, expectedStableVersion = "") {
    if (!xml || Buffer.byteLength(xml, "utf8") < 500) {
        fail("the generated file is empty or implausibly small");
    }

    assertWellFormedStructure(xml);

    const completeDocumentPattern =
        /^<\?xml[^>]*>\s*<praktiKST>[\s\S]*<\/praktiKST>\s*$/;

    if (!completeDocumentPattern.test(xml)) {
        fail(
            "the document does not contain one complete "
            + "<praktiKST> root element"
        );
    }

    const latestVersionBlock = extractTag(xml, "latestVersion");

    if (latestVersionBlock === null) {
        fail("<latestVersion> is missing");
    }

    for (const tagName of [
        "versionNumber",
        "semanticVersion",
        "adminMessage",
        "majorChanges",
        "latestVersionPathOnWebserver"
    ]) {
        assertContainsTag(latestVersionBlock, tagName);
    }

    const legacyVersion =
        extractTag(latestVersionBlock, "versionNumber");

    const semanticVersion =
        extractTag(latestVersionBlock, "semanticVersion");

    const releaseUrl =
        extractTag(
            latestVersionBlock,
            "latestVersionPathOnWebserver"
        );

    if (
        !legacyVersion
        || !/^\d+(?:\.\d+)?$/.test(legacyVersion)
    ) {
        fail(
            "<versionNumber> is not a valid legacy numeric version"
        );
    }

    if (
        !semanticVersion
        || !/^\d+\.\d+(?:\.\d+)?$/.test(semanticVersion)
    ) {
        fail("<semanticVersion> is not a valid Stable version");
    }

    // Feeds published before the move to the HamRadioOnline
    // organisation still carry the old repository owner.
    const releaseUrlPrefixes = [
        "https://github.com/HamRadioOnline/"
        + "kst4contest/releases/tag/",
        "https://github.com/praktimarc/"
        + "kst4contest/releases/tag/"
    ];
    if (
        !releaseUrl
        || !releaseUrlPrefixes.some(
            (prefix) => releaseUrl.startsWith(prefix)
        )
    ) {
        fail(
            "<latestVersionPathOnWebserver> is not "
            + "a KST4Contest release URL"
        );
    }

    const changeLogs = [
        ...xml.matchAll(
            /<changeLog>([\s\S]*?)<\/changeLog>/g
        )
    ].map((match) => match[1]);

    if (changeLogs.length === 0) {
        fail(
            "the document does not contain any "
            + "<changeLog> entries"
        );
    }

    for (const entry of changeLogs) {
        for (const tagName of [
            "changedVersionNumber",
            "date",
            "description",
            "added",
            "changed",
            "fixed",
            "removed"
        ]) {
            assertContainsTag(entry, tagName);
        }
    }

    const expected = normaliseVersion(expectedStableVersion);

    if (expected) {
        if (semanticVersion !== expected) {
            fail(
                `latest Stable version ${semanticVersion} `
                + `does not match expected release ${expected}`
            );
        }

        const releaseEntryExists = changeLogs.some(
            (entry) =>
                extractTag(
                    entry,
                    "changedVersionNumber"
                ) === expected
        );

        if (!releaseEntryExists) {
            fail(
                "the changelog does not contain the expected "
                + `Stable release ${expected}`
            );
        }
    }

    return {
        semanticVersion,
        changeLogEntries: changeLogs.length
    };
}

function parseArguments(argv) {
    const result = {
        file: DEFAULT_FILE,
        expectedStableVersion:
            process.env.EXPECTED_STABLE_VERSION || ""
    };

    for (let index = 0; index < argv.length; index++) {
        if (
            argv[index] === "--file"
            && argv[index + 1]
        ) {
            result.file = path.resolve(argv[++index]);
        } else if (
            argv[index] === "--expected-stable"
            && argv[index + 1]
        ) {
            result.expectedStableVersion = argv[++index];
        } else {
            fail(
                `unknown or incomplete argument: ${argv[index]}`
            );
        }
    }

    return result;
}

if (require.main === module) {
    try {
        const options =
            parseArguments(process.argv.slice(2));

        const xml =
            fs.readFileSync(options.file, "utf8");

        const result =
            validateVersionInfo(
                xml,
                options.expectedStableVersion
            );

        console.log(
            `[versionInfo validation] OK: `
            + `Stable ${result.semanticVersion}, `
            + `${result.changeLogEntries} changelog entries, `
            + options.file
        );
    } catch (err) {
        console.error(err.message);
        process.exitCode = 1;
    }
}

module.exports = {
    normaliseVersion,
    validateVersionInfo
};
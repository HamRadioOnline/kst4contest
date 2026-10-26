const fs = require("fs");
const path = require("path");
const markdownIt = require("markdown-it");
const markdownItAnchor = require("markdown-it-anchor");

const MANUAL_PAGE_ORDER = {
    de: [
        "home",
        "installation",
        "konfiguration",
        "contest-workflow",
        "log-synchronisation",
        "airscout-integration",
        "dx-cluster-server",
        "funktionen",
        "makros-und-variablen",
        "benutzeroberflaeche",
        "changelog"
    ],
    en: [
        "home",
        "installation",
        "configuration",
        "contest-workflow",
        "log-sync",
        "airscout-integration",
        "dx-cluster-server",
        "features",
        "macros-and-variables",
        "user-interface",
        "changelog"
    ]
};

// Removes HTML tags until none are left, so that nested input such as
// "<<b>script>" cannot reassemble a tag after a single pass.
function stripHtmlTags(value) {
    let previous;
    let current = value;

    do {
        previous = current;
        current = current.replace(/<[^>]+>/g, "");
    } while (current !== previous);

    return current;
}

function markdownToPlainText(value) {
    const withoutLinks = (value || "")
        .replace(/!\[([^\]]*)\]\([^)]+\)/g, "$1")
        .replace(/\[([^\]]+)\]\([^)]+\)/g, "$1");

    return stripHtmlTags(withoutLinks)
        .replace(/[*_~`]/g, "")
        .replace(/\s+/g, " ")
        .trim();
}

function extractManualTitle(raw, fallbackTitle) {
    const heading = (raw || "").match(/^#\s+(.+)$/m);
    return heading ? markdownToPlainText(heading[1]) : fallbackTitle;
}

function extractManualSummary(raw) {
    const blocks = (raw || "").split(/\r?\n\s*\r?\n/);

    for (const block of blocks) {
        const trimmed = block.trim();

        if (!trimmed
            || /^(#|>|\||---|```|!\[)/.test(trimmed)
            || /^[-*+]\s/.test(trimmed)
            || /^\d+[.)]\s/.test(trimmed)) {
            continue;
        }

        return markdownToPlainText(trimmed);
    }

    return "";
}

function getManualPageOrder(lang, slug) {
    const configuredOrder = MANUAL_PAGE_ORDER[lang] || [];
    const index = configuredOrder.indexOf(slug);

    return index >= 0 ? index : 999;
}

function selectLatestNews(newsItems) {
    return [...(newsItems || [])]
        .sort((first, second) => second.date - first.date)
        .slice(0, 1);
}

function githubCompatibleSlug(value) {
    return (value || "")
        .trim()
        .toLowerCase()
        .replace(/[^\p{L}\p{N}\s_-]/gu, "")
        .replace(/\s+/g, "-");
}

function rewriteManualLinks(content, lang) {
    return (content || "")

        // Manual images are stored next to the Markdown sources in github_docs.
        // The website publishes them from one shared, language-neutral directory.
        .replace(
            /!\[([^\]]*)\]\((?!https?:\/\/|\/)([^)\s]+\.(?:png|jpe?g|gif|svg|webp))\)/gi,
            (match, altText, imagePath) => {
                const fileName = path.posix.basename(
                    imagePath.replace(/\\/g, "/")
                );

                return `![${altText}](/manual/assets/${fileName})`;
            }
        )

        // GitHub-Wiki-Links im Stil [[de-Installation]] oder [[en-Installation]]
        .replace(/\[\[(en|de)-([^\]|]+)\]\]/g, (match, linkLang, slug) => {
            return `[${slug.replace(/-/g, " ")}](/manual/${linkLang}/${slug.toLowerCase()}/)`;
        })

        // GitHub-Wiki-Links mit Label: [[Text|de-Installation]]
        .replace(/\[\[([^\]|]+)\|(en|de)-([^\]]+)\]\]/g, (match, label, linkLang, slug) => {
            return `[${label}](/manual/${linkLang}/${slug.toLowerCase()}/)`;
        })

        // Markdown links to German or English manual pages.
        // The source may contain an optional ".md" suffix and an optional anchor.
        // Both forms must be converted to the corresponding website manual URL.
        .replace(
            /\]\((en|de)-([^#)]+?)(?:\.md)?(?:#([^)]+))?\)/g,
            (match, linkLang, slug, anchor) => {
                const fragment = anchor ? `#${anchor}` : "";

                return `](/manual/${linkLang}/${slug.toLowerCase()}/${fragment})`;
            }
        );
}

module.exports = function (eleventyConfig) {

    eleventyConfig.addFilter("dateToIso", function (dateObj) {
        return new Date(dateObj).toISOString().split("T")[0];
    });

    eleventyConfig.addFilter("dateToRfc822", function (dateObj) {
        return new Date(dateObj).toUTCString();
    });

    eleventyConfig.addFilter("readableDate", function (dateObj) {
        return new Intl.DateTimeFormat("en", {
            year: "numeric",
            month: "long",
            day: "numeric"
        }).format(dateObj);
    });

    eleventyConfig.addCollection("sortedFeatures", function (collectionApi) {
        return collectionApi.getFilteredByTag("features").sort((a, b) => {
            return (a.data.order || 999) - (b.data.order || 999);
        });
    });

    eleventyConfig.addCollection("featuredFeatures", function (collectionApi) {
        return collectionApi.getFilteredByTag("features")
            .filter(item => item.data.featured)
            .sort((a, b) => {
                return (a.data.order || 999) - (b.data.order || 999);
            });
    });

    eleventyConfig.addCollection("latestNews", function (collectionApi) {
        return selectLatestNews(
            collectionApi.getFilteredByTag("news")
        );
    });

    eleventyConfig.addFilter("whereTag", function(collection, tag) {
        return collection.filter(item => item.data.tags && item.data.tags.includes(tag));
    });

    eleventyConfig.addPassthroughCopy({ "src/assets": "assets" });

    eleventyConfig.addPassthroughCopy({
        "../github_docs/*.{png,jpg,jpeg,gif,svg,webp}": "manual/assets"
    });

    const md = markdownIt({
        html: true,
        linkify: true,
        typographer: true
    }).use(markdownItAnchor, {
        slugify: githubCompatibleSlug,
        permalink: markdownItAnchor.permalink.headerLink()
    });

    eleventyConfig.addFilter("markdown", function (value) {
        return md.render(value || "");
    });

    eleventyConfig.addCollection("manualPages", function () {
        const docsDir = path.join(__dirname, "..", "github_docs");

        if (!fs.existsSync(docsDir)) {
            return [];
        }

        return fs.readdirSync(docsDir)
            .filter(file => /^(en|de)-.+\.md$/.test(file))
            .map(file => {
                const fullPath = path.join(docsDir, file);
                const raw = fs.readFileSync(fullPath, "utf8");

                const lang = file.startsWith("de-") ? "de" : "en";
                const slug = file
                    .replace(/^en-/, "")
                    .replace(/^de-/, "")
                    .replace(/\.md$/, "")
                    .toLowerCase();

                const fallbackTitle = file
                    .replace(/^en-/, "")
                    .replace(/^de-/, "")
                    .replace(/\.md$/, "")
                    .replace(/-/g, " ");

                const title = extractManualTitle(raw, fallbackTitle);

                return {
                    file,
                    lang,
                    slug,
                    title,
                    summary: extractManualSummary(raw),
                    order: getManualPageOrder(lang, slug),
                    content: rewriteManualLinks(raw, lang)
                };
            })
            .sort((a, b) => {
                return a.lang.localeCompare(b.lang)
                    || a.order - b.order
                    || a.title.localeCompare(b.title);
            });
    });

    return {
        dir: {
            input: "src",
            output: "_site",
            includes: "_includes",
            layouts: "_layouts"
        },
        markdownTemplateEngine: "njk",
        htmlTemplateEngine: "njk"
    };
};

module.exports.selectLatestNews = selectLatestNews;

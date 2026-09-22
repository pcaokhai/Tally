import { launch } from "chrome-launcher";
import lighthouse from "lighthouse";

const PERFORMANCE_THRESHOLD = 0.9;
const ACCESSIBILITY_THRESHOLD = 1.0;

function parseUrl(argv) {
  const flagIndex = argv.indexOf("--url");
  if (flagIndex !== -1 && argv[flagIndex + 1]) {
    return argv[flagIndex + 1];
  }
  return "http://localhost:3000";
}

const url = parseUrl(process.argv.slice(2));
const chrome = await launch({ chromeFlags: ["--headless"] });

try {
  const result = await lighthouse(url, {
    port: chrome.port,
    onlyCategories: ["performance", "accessibility"],
    output: "json",
  });

  const { performance, accessibility } = result.lhr.categories;
  console.log(`performance: ${performance.score}`);
  console.log(`accessibility: ${accessibility.score}`);

  const failing = [performance, accessibility].some(
    (category) =>
      (category === performance && category.score < PERFORMANCE_THRESHOLD) ||
      (category === accessibility && category.score < ACCESSIBILITY_THRESHOLD),
  );

  if (failing) {
    for (const category of [performance, accessibility]) {
      const threshold = category === performance ? PERFORMANCE_THRESHOLD : ACCESSIBILITY_THRESHOLD;
      if (category.score >= threshold) continue;
      console.error(`\n${category.title} scored ${category.score}, below ${threshold}`);
      for (const ref of category.auditRefs) {
        const audit = result.lhr.audits[ref.id];
        if (audit && audit.score !== null && audit.score < 1) {
          console.error(`  - [${audit.score}] ${audit.title}`);
        }
      }
    }
    process.exitCode = 1;
  }
} finally {
  await chrome.kill();
}

import { describe, expect, it } from "vitest";
import { detectUnsafeCode, extractSimpleState, parseSfcSections } from "@/utils/sfc";

describe("sfc utils", () => {
  it("parses sections", () => {
    const code = `
<template><div>{{ msg }}</div></template>
<script setup>
const msg = 'hello';
</script>
<style scoped>.x{}</style>
`;
    const parsed = parseSfcSections(code);
    expect(parsed.template).toContain("<template>");
    expect(parsed.script).toContain("<script setup>");
    expect(parsed.style).toContain("<style scoped>");
  });

  it("detects unsafe code", () => {
    const code = "<script setup>eval('1')</script>";
    expect(detectUnsafeCode(code)).toBe(true);
  });

  it("extracts simple refs", () => {
    const script = "<script setup>const count = ref(1); const title = 'demo';</script>";
    const state = extractSimpleState(script);
    expect(state.count).toBe(1);
    expect(state.title).toBe("demo");
  });

  it("extracts array/object demo data from ref", () => {
    const script = `
<script setup>
const books = ref([
  { id: 1, title: '三体', price: 89.0, status: 'available' },
  { id: 2, title: '人类简史', price: 68.0, status: 'borrowed' }
]);
</script>
`;
    const state = extractSimpleState(script);
    expect(Array.isArray(state.books)).toBe(true);
    expect(state.books.length).toBe(2);
    expect(state.books[0].price).toBe(89);
    expect(state.books[1].title).toBe("人类简史");
  });
});

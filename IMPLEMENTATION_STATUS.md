# CASL Android Implementation Status

Comparison between casl-js and casl-android implementations based on spec files.

## ✅ Implemented Features

### Core Ability Features
- [x] Basic `can()` and `cannot()` permission checks
- [x] Rules with actions and subjects
- [x] Inverted rules (cannot)
- [x] Wildcard actions (`manage`)
- [x] Wildcard subjects (`all`)
- [x] Array actions (e.g., `["read", "update"]`)
- [x] Last-match-wins rule precedence
- [x] String subject type literals
- [x] Update rules via `update()` method
- [x] Export rules via `exportRules()`

### Subject Detection
- [x] String subjects (type literals)
- [x] Object subjects (using class name)
- [x] Forced subjects via `subject()` helper
- [x] Custom subject type detection

### Conditions & Operators
- [x] Basic equality conditions
- [x] MongoDB-style operators:
  - [x] `$eq` - equals
  - [x] `$ne` - not equals
  - [x] `$gt`, `$gte` - greater than (or equal)
  - [x] `$lt`, `$lte` - less than (or equal)
  - [x] `$in` - in array
  - [x] `$nin` - not in array
  - [x] `$exists` - field exists
  - [x] `$regex` - regex matching
  - [x] `$size` - array size
  - [x] `$all` - array contains all
  - [x] `$elemMatch` - element matches

### Builder Pattern
- [x] `Ability.builder()` DSL
- [x] `can()` method
- [x] `cannot()` method
- [x] `build()` method

## ❌ Missing Features

### Action Aliases
- [ ] `createAliasResolver()` - not implemented
- [ ] Action alias support (e.g., `modify` -> `[update, delete]`)
- [ ] Nested aliases
- [ ] Alias validation (no cycles, no `manage` alias)

### Custom Options
- [ ] Custom `anyAction` option (hardcoded to "manage")
- [ ] Custom `anySubjectType` option (hardcoded to "all")
- [ ] Custom `resolveAction` option

### Events
- [ ] Event system (`on()`, `off()`)
- [ ] `update` event
- [ ] `updated` event
- [ ] Unsubscribe functionality

### Field-Level Permissions
- [ ] Field restrictions in rules
- [ ] `can(action, subject, field)` - field parameter exists but needs testing
- [ ] `permittedFieldsOf()` utility

### Advanced Subject Handling
- [ ] Class-based subjects (using actual classes, not just strings)
- [ ] Subject type detection strategies
- [ ] `detectSubjectType` custom function

### Rule Conditions
- [ ] Dot notation paths in conditions (e.g., `"author.id"`)
- [ ] Complex nested condition matching

### Other Utilities
- [ ] `packRules()` - rule compression
- [ ] `unpackRules()` - rule decompression
- [ ] `rulesToQuery()` - convert rules to query
- [ ] `rulesToAST()` - convert rules to AST
- [ ] `ForbiddenError` with detailed error messages

## 📊 Test Coverage Comparison

### casl-js (ability.spec.js)
- ~60+ test cases covering all features
- Event system tests
- Action alias tests
- Field-level permission tests
- Rule precedence tests
- Complex condition tests

### casl-android
- ~64 test cases implemented
- Core ability tests ✅
- Array action tests ✅
- Operator tests ✅
- Subject helper tests ✅
- Missing: Events, aliases, field-level, advanced features

## 🎯 Priority Recommendations

### High Priority (Core Functionality)
1. **Field-level permissions** - Important for fine-grained access control
2. **Dot notation in conditions** - Common use case for nested objects
3. **Action aliases** - Useful for grouping related actions

### Medium Priority (Developer Experience)
4. **Events** - Useful for reactive UIs
5. **Custom options** - Flexibility for different use cases
6. **ForbiddenError** - Better error messages

### Low Priority (Advanced Features)
7. **Rule packing** - Optimization for network transfer
8. **Class-based subjects** - Less common in Android/Kotlin
9. **AST/Query conversion** - Specialized use cases

## 📝 Notes

- The current implementation covers the most common use cases (80/20 rule)
- Missing features are mostly advanced or optimization-related
- The library is functional and production-ready for basic-to-intermediate use cases
- Operators implementation matches casl-js behavior

## Next Steps

1. Review this document with stakeholders
2. Prioritize missing features based on use cases
3. Implement high-priority features
4. Add corresponding tests from casl-js spec folder
